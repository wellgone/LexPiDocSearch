package top.lvpi.model.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(name = "LbImgEntity", description = "图书封面图片表")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@TableName("lp_img")
public class Img {

    //图片id        
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "图片id", name = "id", type = "Long")
    private Long id;
  
    //图片base64数据        
    @Schema(description = "图片base64数据", name = "imgData", type = "String")
    @TableField(value = "img_data")
    private String imgData;
    
    //创建时间        
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间", name = "createTime", type = "Date")
    private Date createTime;
    
    //修改时间        
    @TableField(value = "modified_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改时间", name = "modifiedTime", type = "Date")
    private Date modifiedTime;
 
    //图片所属书籍id        
    @Schema(description = "图片所属书籍id", name = "bookId", type = "Long")
    @TableField(value = "book_id")
    private Long bookId;
    
    //是否删除
    @TableLogic
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    @Schema(description = "是否删除", name = "isDeleted", type = "Integer")
    private Integer isDeleted;
}

