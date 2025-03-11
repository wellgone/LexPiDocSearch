package top.lvpi.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @ClassName: CustomPDFTextStripper
 * @Description: 自定义PDFTextStripper类，对写出数据进行处理，删除无意义的换行
 * @Author well
 * @Date: 2024/11/5 13:36
 * @Version 1.0
 */
public class CustomPDFTextStripper extends PDFTextStripper {
    public CustomPDFTextStripper() throws IOException {
    }

    @Override
    public void writeText(PDDocument doc, Writer outputStream) throws IOException {
        // 使用StringWriter来收集提取的文本
        StringWriter writer = new StringWriter();

        // 调用父类的writeText方法，将内容写入StringWriter
        super.writeText(doc, writer);

        // 获取从StringWriter中提取的文本
        String extractedText = writer.toString();
        String[] split = extractedText.split("\\n");

        int totalLength = 0;
        int count = 0;

        // 计算每行的字数
        int[] lineLengths = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            lineLengths[i] = split[i].replaceAll("\\s", "").length(); // 移除空白字符
        }

        // 计算平均字数
        int sum = 0;
        for (int length : lineLengths) {
            sum += length;
        }
        double averageLength = (double) sum / split.length;
        // 过滤满行并计算平均字数
        for (int length : lineLengths) {
            if (length >= averageLength) { // 假设长度大于等于平均长度的为满行
                totalLength += length;
                count++;
            }
        }
        int fullLineAverageLength = totalLength / count;
//        System.out.println("Average length of full lines: " + fullLineAverageLength);
        //用于储存处理后是行数据
        StringBuilder dealContent = new StringBuilder();
        //遍历处理行数据
        for (String item: split){
            if (item.trim().matches("^(\\d+\\.|\\d+、|\\d+\\)|（[一二三四五六七八九十]）|[一二三四五六七八九十] |第([一二三四五六七八九十])|[一二三四五六七八九十]、|\\([一二三四五六七八九十]\\)、|\\(\\d+\\)|①|②|③|④|⑤|⑥|⑦|⑧|⑨).*") && item.replaceAll("\\s", "").length() < fullLineAverageLength-2){
                //1. 行以常见序号开头的，且行字数未达到平均最大行字数的，视为标题,则换行
                //先判断前一个item是否以换行符结尾，如果不是则加上
                if (dealContent.length()>0 && !dealContent.subSequence(dealContent.length() - 1, dealContent.length()).equals("\n")){
                    dealContent.append("\n");
                }
                dealContent.append(item.trim()).append("\n");
            }else if (item.replaceAll(" ", "").trim().matches("(^①|^②|^③|^④|^⑤|^⑥|^⑦|^⑧|^⑨).*")){
                //先判断dealContent是否为空，不为空才操作
                if (dealContent.length()!=0){
                    //当以圆圈序号开头，直接判断前一个item是否以换行符结尾，如果不是则加上
                    if (!dealContent.subSequence(dealContent.length() - 1, dealContent.length()).equals("\n")){
                        dealContent.append("\n");
                    }
                }
                //删除行内空及不可见符号
                item = item.replaceAll("\\s", "").replaceAll(" ", "");
                dealContent.append(item.trim());
            }else if (item.trim().matches(".*[。！：:;；①②③④⑤⑥⑦⑧⑨]$")&& item.replaceAll("\\s", "").length() < fullLineAverageLength){
                //2. 以。！：:;；①②③④⑤⑥⑦⑧⑨结尾，未填满整行的，直接视为段落，则换行
                dealContent.append(item.trim()).append("\n");
            }else {
                //3. 其余全部换行都均进行合并不换行
                //删除行内空及不可见符号
                item = item.replaceAll("\\s", "").replaceAll(" ", "");
                dealContent.append(item.trim());
            }
        }
        outputStream.write(dealContent.toString());
    }
//
//    // 清理文本中的换行符
//    private String cleanText(String text) {
//        // 1. 删除段落内部的无意义换行符
//        text = text.replaceAll("(?<=\\S)\n(?=\\S)", " ");  // 连续非空字符的换行符替换为空格
//
//        // 2. 替换多个连续的换行符为单个换行符
//        text = text.replaceAll("\n+", "\n");  // 将多个连续的换行符合并为一个
//
//        // 3. 去除空白行
//        text = text.replaceAll("(?m)^[ \t]*\r?\n", "");
//
//        return text;
//    }
}
