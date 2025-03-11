package top.lvpi.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "批量删除笔记请求")
public class BatchDeleteNoteRequest {
    @Schema(description = "笔记ID列表")
    private List<Long> noteIds;
} 