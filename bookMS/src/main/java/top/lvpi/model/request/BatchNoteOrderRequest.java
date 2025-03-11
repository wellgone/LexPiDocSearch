package top.lvpi.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "批量更新笔记排序请求")
public class BatchNoteOrderRequest {
    @Schema(description = "笔记排序列表")
    private List<NoteOrder> notes;

    @Data
    @Schema(description = "笔记排序信息")
    public static class NoteOrder {
        @Schema(description = "笔记ID")
        private Long id;

        @Schema(description = "排序序号")
        private Integer orderNum;
    }
} 