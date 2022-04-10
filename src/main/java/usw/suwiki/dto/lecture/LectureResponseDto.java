package usw.suwiki.dto.lecture;

import usw.suwiki.domain.lecture.Lecture;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LectureResponseDto {
    private Long id;

    private String semester;
    private String professor;
    private String lectureType; //이수 구분
    private String lectureName;

    private float lectureTotalAvg;
    private float lectureSatisfactionAvg;
    private float lectureHoneyAvg;
    private float lectureLearningAvg;

    public LectureResponseDto(Lecture entity) {
        this.id = entity.getId();
        this.semester = entity.getSemester();
        this.professor = entity.getProfessor();
        this.lectureType = entity.getLectureType();
        this.lectureName = entity.getLectureName();
        this.lectureTotalAvg = entity.getLectureTotalAvg();
        this.lectureSatisfactionAvg = entity.getLectureSatisfactionAvg();
        this.lectureHoneyAvg = entity.getLectureHoneyAvg();
        this.lectureLearningAvg = entity.getLectureLearningAvg();
    }
}