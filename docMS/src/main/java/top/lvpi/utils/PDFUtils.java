package top.lvpi.utils;

import top.lvpi.model.entity.DocSection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PDFUtils {

    /**
     * 提取PDF文本到章节列表
     *
     * @param pdfStream PDF文件输入流
     * @param docId    图书ID
     * @return 章节列表
     * @throws IOException IO异常
     */
    public static List<DocSection> extractTextToSections(InputStream pdfStream, Long docId, String title) throws IOException {
        List<DocSection> sections = new ArrayList<>();
        PDDocument document = PDDocument.load(pdfStream);
        PDFTextStripper stripper = new PDFTextStripper();

        int totalPages = document.getNumberOfPages();
        for (int i = 1; i <= totalPages; i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            String pageText = stripper.getText(document);

            // 如果内容为空、null、空字符串、空格或只有换行符，则跳过
            if (pageText == null || pageText.isEmpty() || pageText.trim().isEmpty() || pageText.trim().equals("\n")|| pageText.trim().equals("\r")) {
                continue;
            }

            DocSection section = new DocSection();
            section.setDocId(docId);
            section.setPageNum(i);
            section.setContent(pageText);
            section.setTitle(title);
            sections.add(section);
        }

        document.close();
        return sections;
    }

    /**
     * 获取PDF文件页数
     *
     * @param pdfStream PDF文件输入流
     * @return 页数
     * @throws IOException IO异常
     */
    public static int getPageCount(InputStream pdfStream) throws IOException {
        PDDocument document = PDDocument.load(pdfStream);
        int pageCount = document.getNumberOfPages();
        document.close();
        return pageCount;
    }

    /**
     * 导出PDF首页为图片
     *
     * @param pdfStream PDF文件输入流
     * @return Base64编码的图片字符串
     * @throws IOException IO异常
     */
    public static String exportPageToImage(InputStream pdfStream) throws IOException {
        PDDocument document = PDDocument.load(pdfStream);
        PDFRenderer renderer = new PDFRenderer(document);
        
        // 渲染第一页
        BufferedImage image = renderer.renderImageWithDPI(0, 150); // 使用150 DPI以获得较好的图像质量
        
        // 将图像转换为Base64字符串
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        
        document.close();
        baos.close();
        
        return base64Image;
    }

}
