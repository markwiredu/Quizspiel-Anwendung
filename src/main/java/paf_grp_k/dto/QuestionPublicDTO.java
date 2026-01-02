package paf_grp_k.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuestionPublicDTO {
    private Long id;
    private String category;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
}
