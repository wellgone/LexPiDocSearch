package top.lvpi.model.dto.book;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "图书更新请求")
public class BookUpdateRequest {
    @Schema(description = "书籍ID")
    private Long id;

    @Schema(description = "书名")
    private String title;

    @Schema(description = "副标题")
    private String subTitle;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "出版社")
    private String publisher;

    @Schema(description = "出版年份")
    private String publicationYear;

    @Schema(description = "出版日期")
    private String publicationDate;

    @Schema(description = "ISBN")
    private String isbn;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "关键词")
    private String keyWord;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "封面URL")
    private String picUrl;

    @Schema(description = "是否OCR")
    private Integer isOcr;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "页数")
    private Integer pageSize;

    @Schema(description = "MD5")
    private String md5;

    @Schema(description = "中图分类号")
    private String cn;

    @Schema(description = "丛编")
    private String series;

    @Schema(description = "评分")
    private Integer score;

    @Schema(description = "是否已抽取文本")
    private Integer isExtracted;

    @Schema(description = "OPAC主题")
    private String topic;

    @Schema(description = "OPAC丛编")
    private String opacSeries;

    @Schema(description = "是否已获取OPAC")
    private Integer isOpaced;

    @Schema(description = "ISBN格式化")
    private String isbnFormat;

    @Schema(description = "文件ID")
    private Long fileId;

    /**
     * 是否已索引到ES（0：未索引，1：已索引）
     */
    private Integer isIndexed;

    public Integer getIsIndexed() {
        return isIndexed;
    }

    public void setIsIndexed(Integer isIndexed) {
        this.isIndexed = isIndexed;
    }
} 