package top.lvpi.model.dto.task;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TaskProgress {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 任务状态：0-进行中，1-已完成，2-失败
     */
    private Integer status;
    
    /**
     * 进度百分比（0-100）
     */
    private Integer progress;
    
    /**
     * 当前处理的步骤描述
     */
    private String currentStep;
    
    /**
     * 结果信息
     */
    private String result;
    
    /**
     * 错误信息
     */
    private String errorMessage;
} 