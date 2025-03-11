package top.lvpi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lp_note_report_relate")
@Schema(description = "笔记与检索报告关系实体")
public class NoteReportRelate {
    @TableId(type = IdType.AUTO)
    @Schema(description = "关系ID")
    private Long id;

    @Schema(description = "笔记ID")
    private Long noteId;

    @Schema(description = "报告ID")
    private Long reportId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "用户ID")
    private Long userId;
} 