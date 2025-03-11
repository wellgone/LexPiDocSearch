package top.lvpi.model.entity;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Schema(name = "Doc", description = "图书管理表")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@TableName("lp_doc")
public class Doc {
    
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "书籍的唯一标识符")
    private Long id;

    @Schema(description = "书名")
    private String title;

    @Schema(description = "副标题")
    @TableField("sub_title")
    private String subTitle;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "出版社")
    private String publisher;

    @Schema(description = "出版年份")
    @TableField("publication_year")
    private String publicationYear;

    @Schema(description = "出版日期")
    @TableField("publication_date")
    private String publicationDate;

    @Schema(description = "国际标准书号")
    private String isbn;

    @Schema(description = "书籍分类")
    private String category;

    @Schema(description = "主题词/关键词")
    @TableField("key_word")
    private String keyWord;

    @Schema(description = "书籍摘要")
    private String summary;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "minio文件储存名称，唯一")
    @TableField("file_name")
    private String fileName;

    @Schema(description = "封面链接")
    @TableField("pic_url")
    private String picUrl;

    @Schema(description = "是否ocr")
    @TableField("is_ocr")
    private Integer isOcr;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "页数")
    @TableField("page_size")
    private Integer pageSize;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "最后更新时间")
    @TableField(value = "modified_time", fill = FieldFill.INSERT_UPDATE)
    private Date modifiedTime;

    @Schema(description = "md5")
    private String md5;

    @Schema(description = "中图分类号")
    private String cn;

    @Schema(description = "众编")
    private String series;

    @Schema(description = "评分；带不可消除的水印的0，低质量的1，高质量的5")
    private Integer score;

    @Schema(description = "是否已经抽取文本")
    @TableField("is_extracted")
    private Integer isExtracted;

    @Schema(description = "是否已经导入elasticsearch")
    @TableField("is_indexed")
    private Integer isIndexed;

    @Schema(description = "opac主题")
    private String topic;

    @Schema(description = "opac丛编")
    @TableField("opac_series")
    private String opacSeries;

    @Schema(description = "是否已获取opac")
    @TableField("is_opaced")
    private Integer isOpaced;

    @Schema(description = "是否已解析opac主题词")
    @TableField("has_parse_opac_topic")
    private Integer hasParseOPACTopic;

    @Schema(description = "是否已删除")
    @TableField("is_deleted")
    private Integer isDeleted;

    @Schema(description = "isbn格式化")
    @TableField("isbn_format")
    private String isbnFormat;

    @Schema(description = "文件类型，1表示书籍，2表示用户自定义PDF文件")
    @TableField("type")
    private Integer type;


}

