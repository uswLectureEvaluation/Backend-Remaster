package usw.suwiki.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import usw.suwiki.SuwikiVersion;
import usw.suwiki.dto.ToJsonArray;
import usw.suwiki.dto.notice.NoticeResponseDto;
import usw.suwiki.service.lecture.LectureService;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class SuwikiController {

    private final LectureService lectureService;

    @GetMapping("/suwiki-version")
    public ResponseEntity<Integer> findVersionSuwiki(){
        HttpHeaders header = new HttpHeaders();
        int version = SuwikiVersion.version;
        return new ResponseEntity<Integer>(version, header, HttpStatus.valueOf(200));
    }

    @GetMapping("/version-update")
    public ResponseEntity<ToJsonArray> findAllMajorType(){
        HttpHeaders header = new HttpHeaders();
        List<String> list = lectureService.findAllMajorType();
        ToJsonArray data = new ToJsonArray(list);
        return new ResponseEntity<ToJsonArray>(data, header, HttpStatus.valueOf(200));
    }
}
