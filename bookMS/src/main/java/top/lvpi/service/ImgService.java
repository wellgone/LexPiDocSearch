package top.lvpi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.entity.Img;

public interface ImgService extends IService<Img> {
    /**
     * 保存图片
     * @param base64Data base64图片数据
     * @param bookId 书籍ID
     * @return 图片ID
     */
    Long saveImage(String base64Data, Long bookId);

    /**
     * 获取图片
     * @param bookId 书籍ID
     * @return 图片base64数据
     */
    String getImageByBookId(Long bookId);

    /**
     * 根据图片ID获取图片
     * @param imgId 图片ID
     * @return 图片base64数据
     */
    String getImageById(Long imgId);
} 