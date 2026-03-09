package interview.guide.modules.interview.repository;

import interview.guide.modules.interview.pojo.AddQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewAddRepository extends JpaRepository<AddQuestionEntity,Integer> {

}
