package top.lvpi.model.request;

import top.lvpi.model.entity.Note;
import lombok.Data;

@Data
public class CreateNoteRequest {
    private Note note;
    private Long reportId;
} 