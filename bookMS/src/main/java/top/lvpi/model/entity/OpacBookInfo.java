package top.lvpi.model.entity;

import lombok.Data;
import lombok.ToString;

/**
 * @ClassName: OpacBookInfo
 * @Description: TODO
 * @Author well
 * @Date: 2024/11/21 15:13
 * @Version 1.0
 */
@Data
@ToString
public class OpacBookInfo {
    //标题
    private String title;
    //出版社
    private String press;
    //出版年
    private String year;
    //页数
    private String pageSize;
    //摘要
    private String summary;
    //主题
    private String topic;
    //中图分类号
    private String cn;
    //作者
    private String author;
    //众编
    private String series;
}
