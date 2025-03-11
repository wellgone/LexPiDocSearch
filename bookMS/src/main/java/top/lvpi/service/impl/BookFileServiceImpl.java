package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.mapper.BookFileMapper;
import top.lvpi.model.entity.BookFile;
import top.lvpi.model.dto.file.BookFileDTO;
import top.lvpi.service.BookFileService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookFileServiceImpl extends ServiceImpl<BookFileMapper, BookFile> implements BookFileService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveBookFile(BookFileDTO bookFileDTO) {
        BookFile bookFile = new BookFile();
        BeanUtils.copyProperties(bookFileDTO, bookFile);
        save(bookFile);
        return bookFile.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchBookFile(List<BookFileDTO> bookFileDTOList) {
        List<BookFile> bookFiles = bookFileDTOList.stream()
                .map(dto -> {
                    BookFile bookFile = new BookFile();
                    BeanUtils.copyProperties(dto, bookFile);
                    return bookFile;
                })
                .collect(Collectors.toList());
        return saveBatch(bookFiles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBookFile(Long id) {
        if (id == null) {
            return false;
        }
        return removeById(id);
    }

    @Override
    public List<BookFileDTO> getBookFilesByBookId(Long bookId) {
        if (bookId == null) {
            return List.of();
        }
        LambdaQueryWrapper<BookFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookFile::getBookId, bookId);
        return list(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookFileDTO> getBookFilesByFileId(Long fileId) {
        if (fileId == null) {
            return List.of();
        }
        LambdaQueryWrapper<BookFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookFile::getFileId, fileId);
        return list(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private BookFileDTO convertToDTO(BookFile bookFile) {
        BookFileDTO dto = new BookFileDTO();
        BeanUtils.copyProperties(bookFile, dto);
        return dto;
    }

    @Override
    public BookFileDTO getBookFilesNoDeleteByBookId(Long bookId) {
        return getBookFilesByBookId(bookId).stream()
                // 过滤未删除的文件，isDeleted不为1
                .filter(dto -> dto.getIsDeleted() != 1)
                .findFirst()
                .orElse(null);
    }

} 