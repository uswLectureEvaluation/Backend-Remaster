package usw.suwiki.domain.reportTarget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface ExamReportRepository extends JpaRepository<ExamPostReport, Long> {

    @Query(value = "SELECT * FROM exam_post_report", nativeQuery = true)
    List<ExamPostReport> loadAllReportedPosts();

    void deleteByExamIdx(Long examIdx);


    Optional<ExamPostReport> findByExamIdx(Long examIdx);

}