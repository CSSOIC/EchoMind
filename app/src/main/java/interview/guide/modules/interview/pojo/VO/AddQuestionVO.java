package interview.guide.modules.interview.pojo.VO;

import interview.guide.modules.interview.model.InterviewQuestionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddQuestionVO {
    public boolean hasNextAddQuestion;
    public String addQuestionNow;
    public int currentQuestionIndex;
    public int currentAddQuestionIndex;
}
