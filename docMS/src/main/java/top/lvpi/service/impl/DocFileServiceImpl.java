package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.mapper.DocFileMapper;
import top.lvpi.model.entity.DocFile;
import top.lvpi.model.dto.file.DocFileDTO;
import top.lvpi.service.DocFileService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocFileServiceImpl extends ServiceImpl<DocFileMapper, DocFile> implements DocFileService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDocFile(DocFileDTO docFileDTO) {
        DocFile docFile = new DocFile();
        BeanUtils.copyProperties(docFileDTO, docFile);
        save(docFile);
        return docFile.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchDocFile(List<DocFileDTO> docFileDTOList) {
        List<DocFile> docFiles = docFileDTOList.stream()
                .map(dto -> {
                    DocFile docFile = new DocFile();
                    BeanUtils.copyProperties(dto, docFile);
                    return docFile;
                })
                .collect(Collectors.toList());
        return saveBatch(docFiles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocFile(Long id) {
        if (id == null) {
            return false;
        }
        return removeById(id);
    }

    @Override
    public List<DocFileDTO> getDocFilesByDocId(Long docId) {
        if (docId == null) {
            return List.of();
        }
        LambdaQueryWrapper<DocFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocFile::getDocId, docId);
        return list(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocFileDTO> getDocFilesByFileId(Long fileId) {
        if (fileId == null) {
            return List.of();
        }
        LambdaQueryWrapper<DocFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocFile::getFileId, fileId);
        return list(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private DocFileDTO convertToDTO(DocFile docFile) {
        DocFileDTO dto = new DocFileDTO();
        BeanUtils.copyProperties(docFile, dto);
        return dto;
    }

    @Override
    public DocFileDTO getDocFilesNoDeleteByDocId(Long docId) {
        return getDocFilesByDocId(docId).stream()
                // 过滤未删除的文件，isDeleted不为1
                .filter(dto -> dto.getIsDeleted() != 1)
                .findFirst()
                .orElse(null);
    }

} 