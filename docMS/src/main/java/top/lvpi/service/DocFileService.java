package top.lvpi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.entity.DocFile;
import top.lvpi.model.dto.file.DocFileDTO;

import java.util.List;

public interface DocFileService extends IService<DocFile> {
    
    /**
     * 保存书籍文件关联信息
     *
     * @param docFileDTO 书籍文件关联DTO
     * @return 关联ID
     */
    Long saveDocFile(DocFileDTO docFileDTO);

    /**
     * 批量保存书籍文件关联信息
     *
     * @param docFileDTOList 书籍文件关联DTO列表
     * @return 是否保存成功
     */
    boolean saveBatchDocFile(List<DocFileDTO> docFileDTOList);

    /**
     * 删除书籍文件关联信息
     *
     * @param id 关联ID
     * @return 是否删除成功
     */
    boolean deleteDocFile(Long id);

    /**
     * 根据书籍ID获取文件关联列表
     *
     * @param docId 书籍ID
     * @return 文件关联DTO列表
     */
    List<DocFileDTO> getDocFilesByDocId(Long docId);

    /**
     * 根据书籍ID获取未删除的文件关联列表
     *
     * @param docId 书籍ID
     * @return 文件关联DTO列表
     */
    DocFileDTO getDocFilesNoDeleteByDocId(Long docId);

    /**
     * 根据文件ID获取书籍关联列表
     *
     * @param fileId 文件ID
     * @return 文件关联DTO列表
     */
    List<DocFileDTO> getDocFilesByFileId(Long fileId);
} 