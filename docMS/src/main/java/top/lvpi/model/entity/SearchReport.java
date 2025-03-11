package top.lvpi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lp_search_report")
@Schema(description = "检索报告实体")
public class SearchReport {
    @TableId(type = IdType.AUTO)
    @Schema(description = "报告ID")
    private Long id;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "报告类型")
    private String type;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime modifiedTime;

    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "是否设为检索菜单子选项")
    private Integer searchSubject;
} 