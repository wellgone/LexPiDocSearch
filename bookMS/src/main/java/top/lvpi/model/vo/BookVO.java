package top.lvpi.model.vo;

import top.lvpi.model.entity.Book;
import top.lvpi.model.entity.Topic;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class BookVO extends Book {
    /**
     * 图书标签
     */
    private List<Topic> tags;
} 