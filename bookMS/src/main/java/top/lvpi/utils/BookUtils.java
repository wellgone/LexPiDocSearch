package top.lvpi.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import top.lvpi.model.entity.OpacBookInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName: tool
 * @Description: TODO
 * @Author well
 * @Date: 2024/11/21 10:36
 * @Version 1.0
 */
@Slf4j
@Component
public class BookUtils {

    private final List<String> pcWords;
    private final List<String> UAList;

    public BookUtils() {
        // 使用InputStream读取资源文件
        try {
            Resource resource = new ClassPathResource("pc.txt");
            try (InputStream inputStream = resource.getInputStream()) {
                // 使用BufferedReader读取文件内容
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                );
                this.pcWords = reader.lines().collect(Collectors.toList());
                this.UAList = this.pcWords; // 使用同一个文件内容
            }
        } catch (IOException e) {
            log.error("Failed to load pc.txt file", e);
            throw new RuntimeException("Failed to load pc.txt file", e);
        }
    }

    static OkHttpClient client = null;

    static {
        client = new OkHttpClient().newBuilder()
                .connectTimeout(200, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(50, 10, TimeUnit.SECONDS))
                .build();
    }

    //根据isbn查询图书信息
    public OpacBookInfo getBookInfoByISBN(String isbn) throws IOException {
        System.out.println(isbn);

        //1.从服务器获取isbn数据
        Request request = new Request.Builder()
                .url("http://opac.nlc.cn/F?func=find-b&find_code=ISB&request="+isbn)
                .method("GET", null)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .addHeader("Accept-Language", "en,zh-CN;q=0.9,zh-TW;q=0.8,zh;q=0.7,en-US;q=0.6")
                .addHeader("Referer", "http://opac.nlc.cn")
                .addHeader("User-Agent", UAList.get(RandomUtil.randomInt(0,1000)))
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            String html = response.body().string();
            Document document = Jsoup.parse(html);
            Element tbody = document.body().getElementById("td");
            OpacBookInfo opacBook = new OpacBookInfo();
            //2.解析相关字段信息
            Elements tr = null;
            if (tbody != null) {
                tr = tbody.getElementsByTag("tr");
                for (Element element : tr) {
                    if (element.child(0).text().equals("题名与责任")) {
                        //title
                        String title = "";
                        //判断是否存在/
                        if (element.child(1).text().contains("/")) {
                            title = element.child(1).text().split("/")[0].replace("[专著]", "").replaceAll("\\s+", "");
                        } else {
                            title = element.child(1).text();
                        }
                        opacBook.setTitle(title);
                    } else if (element.child(0).text().equals("出版项")) {
                        System.out.println(element.child(1).text());
                        if (element.child(1).text().contains(":")) {
                            //以":"分隔并取最后一个元素
                            String[] split = element.child(1).text().split(":");
                            String item = split[split.length - 1];
                            opacBook.setPress(item.split(",")[0].trim());
                            opacBook.setYear(item.split(",")[1].trim());

                        } else {
                            System.out.println("出版项--待核实！");
                            System.out.println(element.child(1).text());
                        }
                    } else if (element.child(0).text().equals("载体形态项")) {
                        String pageSize = "";
                        String resultData = ReUtil.get("(\\d+)页", element.child(1).text(), 1);
                        if (resultData != null) {
                            pageSize = resultData;
                        }
                        opacBook.setPageSize(pageSize);
                    } else if (element.child(0).text().equals("内容提要")) {
                        String summary = element.child(1).text();
                        opacBook.setSummary(summary);
                    } else if (element.child(0).text().equals("主题")) {
                        String topic = element.child(1).text().replaceAll("\\s+", "");
                        opacBook.setTopic(topic);
                    } else if (element.child(0).text().equals("中图分类号")) {
                        opacBook.setCn(element.child(1).text().trim());
                    } else if (element.child(0).text().equals("著者")) {
                        opacBook.setAuthor(element.child(1).text().trim().split(" ")[0]);
                    } else if (element.child(0).text().equals("丛编项")) {
                        opacBook.setSeries(element.child(1).text().trim());
                    } else if (element.child(0).text().trim().equals("")) {
                        if (element.child(1).text().contains("主编") || element.child(1).text().contains("著")) {
                            System.out.println("作者");
                            opacBook.setAuthor(opacBook.getAuthor() + "," + element.child(1).text().trim().split(" ")[0]);
                        } else if (element.child(1).text().contains("-")) {
                            System.out.println("主题词");
                            opacBook.setTopic(opacBook.getTopic() + "," + element.child(1).text().replaceAll("\\s+", ""));
                        } else if (element.child(1).text().contains(".")) {
                            System.out.println("中图分类号");
                            opacBook.setCn(opacBook.getCn() + "," + element.child(1).text().trim());
                        }
                    }
                }
                return opacBook;
            }else {
                if(document.html().contains("数据库里没有这条请求记录")){
                    log.info("数据库里没有这条请求记录.");
                }else {
                    log.info("未知错误！");
                }
            }
        }else {
            log.info("请求失败");
        }
        return null;
    }

}
