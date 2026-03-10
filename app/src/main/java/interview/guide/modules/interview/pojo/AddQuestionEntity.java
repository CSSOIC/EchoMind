package interview.guide.modules.interview.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class AddQuestionEntity {
    @Id
    public int id;
    public int addQuestionIndex;
    public int questionIndex;
    public String addQuestionAnswer;
    public String addQuestion;
}
