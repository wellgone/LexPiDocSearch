package top.lvpi.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "笔记层级关系请求")
public class NoteHierarchyRequest {
    @Schema(description = "笔记ID")
    private Long noteId;

    @Schema(description = "父级笔记ID")
    private Long parentId;

    @Schema(description = "排序序号")
    private Integer orderNum;
} 