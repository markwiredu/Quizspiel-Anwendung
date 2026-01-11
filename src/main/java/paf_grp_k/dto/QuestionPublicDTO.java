package paf_grp_k.dto;

public class QuestionPublicDTO {

    private Long id;
    private String category;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    public QuestionPublicDTO(Long id, String category, String questionText,
                             String optionA, String optionB, String optionC, String optionD) {
        this.id = id;
        this.category = category;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getQuestionText() { return questionText; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
}
