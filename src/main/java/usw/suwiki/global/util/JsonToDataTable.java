package usw.suwiki.global.util;

import usw.suwiki.domain.lecture.JsonToLectureDto;
import usw.suwiki.domain.lecture.Lecture;
import usw.suwiki.domain.lecture.JpaLectureRepository;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

@RequiredArgsConstructor
@Transactional
@Service
public class JsonToDataTable {

    private final JpaLectureRepository lectureRepository;

    public void toEntity() throws IOException, ParseException, InterruptedException {

        Reader reader = new FileReader("E:/Priority/Project/SUWIKI-REMASTER/src/main/resources/USW_2022_1 thirteen.json");

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(reader);

        JSONArray jsonArray = (JSONArray) obj;

        if(jsonArray.size() > 0 ){
            for(int i=0; i< jsonArray.size(); i++) {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);

                JsonToLectureDto dto = JsonToLectureDto.builder()
                        .capprType((String) jsonObject.get("capprTypeNm"))
                        .evaluateType((String) jsonObject.get("cretEvalNm"))
                        .lectureCode((String) jsonObject.get("subjtCd"))
                        .selectedSemester(jsonObject.get("subjtEstbYear") + "-" + String.valueOf(jsonObject.get("subjtEstbSmrCd")).substring(0, 1))
                        .grade(Integer.parseInt(jsonObject.get("trgtGrdeCd").toString()))
                        .lectureType((String) jsonObject.get("facDvnm"))
                        .placeSchedule(String.valueOf(jsonObject.get("timtSmryCn")))
                        .diclNo(String.valueOf(jsonObject.get("diclNo")))
                        .majorType(String.valueOf(jsonObject.get("estbDpmjNm")))
                        .point(Double.parseDouble(String.valueOf(jsonObject.get("point"))))
                        .professor(String.valueOf(jsonObject.get("reprPrfsEnoNm")))
                        .lectureName(String.valueOf(jsonObject.get("subjtNm")))
                        .build();

                //professor ????????? "-" ??? ?????? (null ??? ???????????? ??????)
                if(dto.getProfessor().isEmpty()){
                    dto.setProfessor("-");
                }

                if(dto.getLectureName().contains("?????????")){
                    dto.setLectureName(dto.getLectureName().replace("(?????????)",""));
                }

                if(dto.getLectureName().contains("???????????????")){
                    dto.setLectureName(dto.getLectureName().replace("(???????????????)",""));
                    dto.setLectureName(dto.getLectureName().replace("???????????????-", ""));
                    dto.setLectureName(dto.getLectureName().replace("???????????????_", ""));
                }

                if (dto.getLectureName().contains("????????????")){
                    dto.setLectureName(dto.getLectureName().replace("(????????????)",""));
                    dto.setLectureName(dto.getLectureName().replace("????????????-", ""));
                    dto.setLectureName(dto.getLectureName().replace("????????????_", ""));
                    dto.setLectureName(dto.getLectureName().replace("????????????", ""));
                }

                if(dto.getLectureName().contains("????????????")){
                    dto.setLectureName(dto.getLectureName().replace("(????????????)",""));
                    dto.setLectureName(dto.getLectureName().replace("????????????-", ""));
                    dto.setLectureName(dto.getLectureName().replace("????????????_", ""));
                }


                //"??" to replace "-"
                if(dto.getMajorType().contains("??")){
                    String majorType = dto.getMajorType();
                    majorType = majorType.replace("??", "-");
                    dto.setMajorType(majorType);
                }

                Lecture lecture = lectureRepository.verifyJsonLecture(dto.getLectureName(), dto.getProfessor(),dto.getMajorType());

                if (lecture != null) {
                    if (!lecture.getSemesterList().contains(dto.getSelectedSemester())) {
                        String updateString = lecture.getSemesterList() + ", " + dto.getSelectedSemester();
                        Lecture updatedLecture = Lecture.builder().build();
                        updatedLecture.toEntity(dto);
                        updatedLecture.setSemester(updateString);  //refactoring ??????
                        lectureRepository.save(updatedLecture);
                    }
                }
                else if (lecture == null){
                    Lecture savedLecture = Lecture.builder().build();
                    savedLecture.toEntity(dto);
                    Thread.sleep(1);
//                    System.out.println(dto.getLectureName());
                    lectureRepository.save(savedLecture);
                }
            }
        }
    }
}