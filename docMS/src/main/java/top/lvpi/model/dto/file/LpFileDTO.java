package top.lvpi.model.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "文件信息DTO")
public class LpFileDTO {
    
    @Schema(description = "文件ID")
    private Long fileId;
    
    @NotBlank(message = "文件存储名称不能为空")
    @Schema(description = "文件存储名称")
    private String fileName;
    
    @NotBlank(message = "文件原名称不能为空")
    @Schema(description = "文件原名称")
    private String fileOriginalName;
    
    @NotBlank(message = "文件扩展名不能为空")
    @Schema(description = "文件扩展名")
    private String fileSuffix;
    
    @NotNull(message = "文件大小不能为空")
    @Schema(description = "文件大小")
    private Long fileSize;
    
    @NotBlank(message = "文件地址不能为空")
    @Schema(description = "文件地址")
    private String fileUrl;
    
    @Schema(description = "文件备注")
    private String fileNote;
    
    @Schema(description = "文件MD5值")
    private String fileMd5;
} 