package top.lvpi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.entity.BookFile;
import top.lvpi.model.dto.file.BookFileDTO;

import java.util.List;

public interface BookFileService extends IService<BookFile> {
    
    /**
     * 保存书籍文件关联信息
     *
     * @param bookFileDTO 书籍文件关联DTO
     * @return 关联ID
     */
    Long saveBookFile(BookFileDTO bookFileDTO);

    /**
     * 批量保存书籍文件关联信息
     *
     * @param bookFileDTOList 书籍文件关联DTO列表
     * @return 是否保存成功
     */
    boolean saveBatchBookFile(List<BookFileDTO> bookFileDTOList);

    /**
     * 删除书籍文件关联信息
     *
     * @param id 关联ID
     * @return 是否删除成功
     */
    boolean deleteBookFile(Long id);

    /**
     * 根据书籍ID获取文件关联列表
     *
     * @param bookId 书籍ID
     * @return 文件关联DTO列表
     */
    List<BookFileDTO> getBookFilesByBookId(Long bookId);

    /**
     * 根据书籍ID获取未删除的文件关联列表
     *
     * @param bookId 书籍ID
     * @return 文件关联DTO列表
     */
    BookFileDTO getBookFilesNoDeleteByBookId(Long bookId);

    /**
     * 根据文件ID获取书籍关联列表
     *
     * @param fileId 文件ID
     * @return 文件关联DTO列表
     */
    List<BookFileDTO> getBookFilesByFileId(Long fileId);
} 