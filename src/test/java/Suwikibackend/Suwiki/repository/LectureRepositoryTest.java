package Suwikibackend.Suwiki.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import usw.suwiki.SuwikiApplication;
import usw.suwiki.domain.lecture.Lecture;
import usw.suwiki.dto.lecture.LectureFindOption;
import usw.suwiki.dto.lecture.LectureListAndCountDto;
import usw.suwiki.repository.evaluation.JpaEvaluatePostsRepository;
import usw.suwiki.repository.exam.JpaExamPostsRepository;
import usw.suwiki.repository.lecture.JpaLectureRepository;
import usw.suwiki.service.evaluation.EvaluatePostsService;

import javax.transaction.Transactional;
import java.util.Optional;


@Transactional
@SpringBootTest(classes = SuwikiApplication.class)
public class LectureRepositoryTest {

    @Autowired
    JpaLectureRepository jpaLectureRepository;

    @Test
    public void findAllLectureByMajorTypeTest() {
        LectureFindOption option = LectureFindOption.builder().majorType(Optional.of("간호학과")).pageNumber(Optional.of(1)).orderOption(Optional.of("modifiedDate")).build();
        LectureListAndCountDto dto = jpaLectureRepository.findAllLectureByMajorType(option);

        System.out.println(dto.getCount());
        for (Lecture lecture : dto.getLectureList()) {
            System.out.println(lecture.getLectureName() + " ," + lecture.getMajorType());
        }
    }
}
