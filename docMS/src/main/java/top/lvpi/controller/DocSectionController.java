package top.lvpi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.lvpi.common.BaseResponse;
import top.lvpi.model.entity.DocSection;
import top.lvpi.service.DocSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "图书章节管理")
@RestController
@RequestMapping("/doc-section")
@RequiredArgsConstructor
public class DocSectionController {

    private final DocSectionService docSectionService;

    @Operation(summary = "分页查询图书章节")
    @GetMapping("/list")
    public BaseResponse<IPage<DocSection>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long docId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) String content) {
        return BaseResponse.success(docSectionService.page(current, size, docId, pageNum, title, content));
    }

    @Operation(summary = "添加图书章节")
    @PostMapping("/add")
    public BaseResponse<String> add(@RequestBody DocSection docSection) {
        docSectionService.save(docSection);
        return BaseResponse.success("ok");
    }

    @Operation(summary = "更新图书章节")
    @PutMapping("/update")
    public BaseResponse<String> update(@RequestBody DocSection docSection) {
        boolean updated = docSectionService.updateWithEs(docSection);
        return BaseResponse.success(String.valueOf(updated));
    }

    @Operation(summary = "删除图书章节")
    @DeleteMapping("/delete/{id}")
    public BaseResponse<String> delete(@PathVariable Long id) {
        boolean removed = docSectionService.deleteWithEs(id);
        return BaseResponse.success(String.valueOf(removed));
    }
} 