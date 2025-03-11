package top.lvpi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.lvpi.common.BaseResponse;
import top.lvpi.model.entity.BookSection;
import top.lvpi.service.BookSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "图书章节管理")
@RestController
@RequestMapping("/book-section")
@RequiredArgsConstructor
public class BookSectionController {

    private final BookSectionService bookSectionService;

    @Operation(summary = "分页查询图书章节")
    @GetMapping("/list")
    public BaseResponse<IPage<BookSection>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) String content) {
        return BaseResponse.success(bookSectionService.page(current, size, bookId, pageNum, title, content));
    }

    @Operation(summary = "添加图书章节")
    @PostMapping("/add")
    public BaseResponse<String> add(@RequestBody BookSection bookSection) {
        bookSectionService.save(bookSection);
        return BaseResponse.success("ok");
    }

    @Operation(summary = "更新图书章节")
    @PutMapping("/update")
    public BaseResponse<String> update(@RequestBody BookSection bookSection) {
        boolean updated = bookSectionService.updateWithEs(bookSection);
        return BaseResponse.success(String.valueOf(updated));
    }

    @Operation(summary = "删除图书章节")
    @DeleteMapping("/delete/{id}")
    public BaseResponse<String> delete(@PathVariable Long id) {
        boolean removed = bookSectionService.deleteWithEs(id);
        return BaseResponse.success(String.valueOf(removed));
    }
} 