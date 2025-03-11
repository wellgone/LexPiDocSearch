package top.lvpi.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "批量更新笔记层级关系请求")
public class BatchNoteHierarchyRequest {
    @Schema(description = "笔记层级关系列表")
    private List<NoteHierarchy> notes;

    @Data
    @Schema(description = "笔记层级关系信息")
    public static class NoteHierarchy {
        @Schema(description = "笔记ID")
        private Long id;

        @Schema(description = "父级笔记ID")
        private Long parentId;

        @Schema(description = "排序序号")
        private Integer orderNum;
    }
} 