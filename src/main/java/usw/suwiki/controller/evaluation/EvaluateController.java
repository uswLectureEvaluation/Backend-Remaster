package usw.suwiki.controller.evaluation;

import usw.suwiki.dto.PageOption;
import usw.suwiki.dto.ToJsonArray;
import usw.suwiki.dto.evaluate.EvaluatePostsSaveDto;
import usw.suwiki.dto.evaluate.EvaluatePostsUpdateDto;
import usw.suwiki.dto.evaluate.EvaluateResponseByLectureIdDto;
import usw.suwiki.dto.evaluate.EvaluateResponseByUserIdxDto;
import usw.suwiki.exception.AccountException;
import usw.suwiki.exception.ErrorType;
import usw.suwiki.jwt.JwtTokenResolver;
import usw.suwiki.jwt.JwtTokenValidator;
import usw.suwiki.service.evaluation.EvaluatePostsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/evaluate-posts")
public class EvaluateController {

    private final EvaluatePostsService evaluatePostsService;
    private final JwtTokenValidator jwtTokenValidator;
    private final JwtTokenResolver jwtTokenResolver;

    @GetMapping("/findByLectureId")
    public ResponseEntity<ToJsonArray> findByLecture(@RequestHeader String Authorization,@RequestParam Long lectureId,
                                                                   @RequestParam(required = false) Optional<Integer> page){
        HttpHeaders header = new HttpHeaders();
        if (jwtTokenValidator.validateAccessToken(Authorization)) {
            if (jwtTokenResolver.getUserIsRestricted(Authorization)) throw new AccountException(ErrorType.USER_RESTRICTED);
            List<EvaluateResponseByLectureIdDto> list = evaluatePostsService.findEvaluatePostsByLectureId(new PageOption(page), lectureId);
            ToJsonArray data = new ToJsonArray(list);
            return new ResponseEntity<ToJsonArray>(data, header, HttpStatus.valueOf(200));
        }else throw new AccountException(ErrorType.TOKEN_IS_NOT_FOUND);
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateEvaluatePosts(@RequestParam Long evaluateIdx, @RequestHeader String Authorization, @RequestBody EvaluatePostsUpdateDto dto){
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        if (jwtTokenValidator.validateAccessToken(Authorization)) {
            if (jwtTokenResolver.getUserIsRestricted(Authorization)) throw new AccountException(ErrorType.USER_RESTRICTED);
            evaluatePostsService.update(evaluateIdx,dto);
            return new ResponseEntity<String>("success", header, HttpStatus.valueOf(200));
        }else throw new AccountException(ErrorType.TOKEN_IS_NOT_FOUND);
    }

    @PostMapping("/write")
    public ResponseEntity<String> saveEvaluatePosts(@RequestParam Long lectureId,@RequestHeader String Authorization,@RequestBody EvaluatePostsSaveDto dto){
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        if (jwtTokenValidator.validateAccessToken(Authorization)) {
            if (jwtTokenResolver.getUserIsRestricted(Authorization)) throw new AccountException(ErrorType.USER_RESTRICTED);
            Long userIdx = jwtTokenResolver.getId(Authorization);
            if (evaluatePostsService.verifyWriteEvaluatePosts(userIdx, lectureId)) {
                evaluatePostsService.save(dto, userIdx , lectureId);
                return new ResponseEntity<String>("success", header, HttpStatus.valueOf(200));
            }else{
                throw new AccountException(ErrorType.POSTS_WRITE_OVERLAP);
            }
        } else throw new AccountException(ErrorType.TOKEN_IS_NOT_FOUND);
    }

    @GetMapping("/findByUserIdx") // 이름 수정 , 널값 처리 프론트
    public ResponseEntity<ToJsonArray> findByUser(@RequestHeader String Authorization,
                                                  @RequestParam(required = false) Optional<Integer> page){
        HttpHeaders header = new HttpHeaders();
        if (jwtTokenValidator.validateAccessToken(Authorization)) {
            if (jwtTokenResolver.getUserIsRestricted(Authorization)) throw new AccountException(ErrorType.USER_RESTRICTED);
            List<EvaluateResponseByUserIdxDto> list = evaluatePostsService.findEvaluatePostsByUserId(new PageOption(page),
                    jwtTokenResolver.getId(Authorization));

            ToJsonArray data = new ToJsonArray(list);
            return new ResponseEntity<ToJsonArray>(data, header, HttpStatus.valueOf(200));

        }else throw new AccountException(ErrorType.TOKEN_IS_NOT_FOUND);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteEvaluatePosts(@RequestParam Long evaluateIdx,@RequestHeader String Authorization){
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        if (jwtTokenValidator.validateAccessToken(Authorization)) {
            if (jwtTokenResolver.getUserIsRestricted(Authorization)) throw new AccountException(ErrorType.USER_RESTRICTED);
            Long userIdx = jwtTokenResolver.getId(Authorization);
            if (evaluatePostsService.verifyDeleteEvaluatePosts(userIdx, evaluateIdx)) {
                evaluatePostsService.deleteById(evaluateIdx);
                return new ResponseEntity<String>("success", header, HttpStatus.valueOf(200));
            }else{
                throw new AccountException(ErrorType.USER_POINT_LACK);
            }
        } else throw new AccountException(ErrorType.TOKEN_IS_NOT_FOUND);
    }
}
