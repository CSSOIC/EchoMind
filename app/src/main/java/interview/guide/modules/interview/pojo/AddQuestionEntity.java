package interview.guide.modules.interview.pojo;

import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public int addQuestionIndex;
    public int questionIndex;
    @Column(length = 2000)
    public String addQuestionAnswer;
    @Column(length = 2000)
    public String addQuestion;
}
