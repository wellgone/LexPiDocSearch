package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.mapper.ImgMapper;
import top.lvpi.model.entity.Img;
import top.lvpi.service.ImgService;
import top.lvpi.utils.ImageUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImgServiceImpl extends ServiceImpl<ImgMapper, Img> implements ImgService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveImage(String base64Data, Long bookId) {
        // 先删除该书籍的旧图片
        LambdaQueryWrapper<Img> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Img::getBookId, bookId);
        this.remove(queryWrapper);

        // 优化图片
        String optimizedBase64 = ImageUtils.optimizeImage(base64Data);

        // 保存新图片
        Img img = new Img();
        img.setImgData(optimizedBase64);
        img.setBookId(bookId);
        this.save(img);
        
        return img.getId();
    }

    @Override
    public String getImageByBookId(Long bookId) {
        LambdaQueryWrapper<Img> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Img::getBookId, bookId);
        Img img = this.getOne(queryWrapper);
        return img != null ? img.getImgData() : null;
    }

    @Override
    public String getImageById(Long imgId) {
        Img img = this.getById(imgId);
        return img != null ? img.getImgData() : null;
    }
} 