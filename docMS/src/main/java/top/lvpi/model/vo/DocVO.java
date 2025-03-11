package top.lvpi.model.vo;

import top.lvpi.model.entity.Doc;
import top.lvpi.model.entity.Topic;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocVO extends Doc {
    /**
     * 图书标签
     */
    private List<Topic> tags;
} 