package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.mapper.LpFileMapper;
import top.lvpi.model.entity.LpFile;
import top.lvpi.model.dto.file.LpFileDTO;
import top.lvpi.service.LpFileService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LpFileServiceImpl extends ServiceImpl<LpFileMapper, LpFile> implements LpFileService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFile(LpFileDTO fileDTO) {
        LpFile file = new LpFile();
        BeanUtils.copyProperties(fileDTO, file);
        save(file);
        return file.getFileId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFile(LpFileDTO fileDTO) {
        if (fileDTO.getFileId() == null) {
            return false;
        }
        LpFile file = new LpFile();
        BeanUtils.copyProperties(fileDTO, file);
        return updateById(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(Long fileId) {
        if (fileId == null) {
            return false;
        }
        return removeById(fileId);
    }

    @Override
    public LpFileDTO getFileById(Long fileId) {
        if (fileId == null) {
            return null;
        }
        LpFile file = getById(fileId);
        if (file == null) {
            return null;
        }
        LpFileDTO fileDTO = new LpFileDTO();
        BeanUtils.copyProperties(file, fileDTO);
        return fileDTO;
    }

    @Override
    public LpFileDTO getFileByMd5(String md5) {
        if (md5 == null || md5.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<LpFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LpFile::getFileMd5, md5);
        LpFile file = getOne(wrapper);
        if (file == null) {
            return null;
        }
        LpFileDTO fileDTO = new LpFileDTO();
        BeanUtils.copyProperties(file, fileDTO);
        return fileDTO;
    }
} 