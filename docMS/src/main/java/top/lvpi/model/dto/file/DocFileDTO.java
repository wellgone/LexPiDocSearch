package top.lvpi.model.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "书籍文件关联DTO")
public class DocFileDTO {
    
    @Schema(description = "关联ID")
    private Long id;
    
    @NotNull(message = "书籍ID不能为空")
    @Schema(description = "书籍ID")
    private Long docId;
    
    @NotNull(message = "文件ID不能为空")
    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "是否删除")
    private int isDeleted;
} 