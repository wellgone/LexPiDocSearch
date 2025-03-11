package top.lvpi.utils;

import lombok.extern.slf4j.Slf4j;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;

@Slf4j
public class ImageUtils {
    
    // 最大宽度
    private static final int MAX_WIDTH = 300;
    // 最大高度
    private static final int MAX_HEIGHT = 300;
    // JPEG压缩质量
    private static final float COMPRESSION_QUALITY = 0.7f;

    /**
     * 优化Base64图片
     * @param base64Data Base64图片数据
     * @return 优化后的Base64图片数据
     */
    public static String optimizeImage(String base64Data) {
        try {
            // 去除base64头部信息
            String pureBase64 = removePrefixIfExists(base64Data);
            
            // 将base64转换为字节数组
            byte[] imageBytes = Base64.getDecoder().decode(pureBase64);
            
            // 读取图片
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                log.error("无法读取图片数据");
                return base64Data;
            }
            
            // 调整图片尺寸
            BufferedImage resizedImage = resizeImage(originalImage);
            
            // 压缩图片质量
            byte[] compressedImageBytes = compressImage(resizedImage);
            
            // 转换回base64
            return Base64.getEncoder().encodeToString(compressedImageBytes);
            
        } catch (Exception e) {
            log.error("图片优化失败", e);
            return base64Data;
        }
    }
    
    /**
     * 去除Base64头部信息
     */
    private static String removePrefixIfExists(String base64Data) {
        if (base64Data.contains(",")) {
            return base64Data.split(",")[1];
        }
        return base64Data;
    }
    
    /**
     * 调整图片尺寸
     */
    private static BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 计算新的尺寸
        int newWidth = originalWidth;
        int newHeight = originalHeight;
        
        // 如果图片超过最大尺寸，按比例缩小
        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            double widthRatio = (double) MAX_WIDTH / originalWidth;
            double heightRatio = (double) MAX_HEIGHT / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);
            
            newWidth = (int) (originalWidth * ratio);
            newHeight = (int) (originalHeight * ratio);
        }
        
        // 如果尺寸没有变化，直接返回原图
        if (newWidth == originalWidth && newHeight == originalHeight) {
            return originalImage;
        }
        
        // 创建新图片
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        
        // 设置图片平滑度
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制图片
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return resizedImage;
    }
    
    /**
     * 压缩图片质量
     */
    private static byte[] compressImage(BufferedImage image) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // 获取JPEG图片编写器
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG image writers found");
        }
        ImageWriter writer = writers.next();
        
        // 设置压缩参数
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(COMPRESSION_QUALITY);
        
        // 写入图片
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(imageOutputStream);
        writer.write(null, new IIOImage(image, null, null), params);
        
        // 清理资源
        writer.dispose();
        imageOutputStream.close();
        
        return outputStream.toByteArray();
    }

} 