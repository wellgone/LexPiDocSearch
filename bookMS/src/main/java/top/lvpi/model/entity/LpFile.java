package top.lvpi.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Schema(name = "File", description = "文件信息表")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@TableName("lp_file")
public class LpFile {
    
    @TableId(value = "file_id", type = IdType.ASSIGN_ID)
    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "文件存储名称")
    @TableField("file_name")
    private String fileName;

    @Schema(description = "文件原名称")
    @TableField("file_original_name")
    private String fileOriginalName;

    @Schema(description = "文件扩展名")
    @TableField("file_suffix")
    private String fileSuffix;

    @Schema(description = "文件大小")
    @TableField("file_size")
    private Long fileSize;

    @Schema(description = "文件地址")
    @TableField("file_url")
    private String fileUrl;

    @Schema(description = "文件备注")
    @TableField("file_note")
    private String fileNote;

    @Schema(description = "文件MD5值")
    @TableField("file_md5")
    private String fileMd5;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "最后更新时间")
    @TableField(value = "modified_time", fill = FieldFill.INSERT_UPDATE)
    private Date modifiedTime;

    @Schema(description = "是否已删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
} 