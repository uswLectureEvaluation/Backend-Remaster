package usw.suwiki.domain.user.quitRequestUser;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usw.suwiki.domain.evaluation.EvaluatePostsService;
import usw.suwiki.domain.exam.ExamPostsService;
import usw.suwiki.domain.user.User;
import usw.suwiki.domain.user.UserRepository;
import usw.suwiki.domain.user.UserService;
import usw.suwiki.domain.userIsolation.UserIsolation;
import usw.suwiki.domain.userIsolation.UserIsolationRepository;
import usw.suwiki.domain.viewExam.ViewExamService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuitRequestUserService {

    // User
    private final UserService userService;
    private final UserRepository userRepository;

    // 휴면 계정
    private final UserIsolationRepository userIsolationRepository;

    // 회원탈퇴 요청 계정
    private final ViewExamService viewExamService;
    private final EvaluatePostsService evaluatePostsService;
    private final ExamPostsService examPostsService;

    //회원탈퇴 요청 유저 일부 데이터 초기화
    @Transactional
    public void disableUser(User user) {
        user.setRestricted(true);
        user.setRestrictedCount(null);
        user.setRole(null);
        user.setWrittenEvaluation(null);
        user.setWrittenExam(null);
        user.setViewExamCount(null);
        user.setPoint(null);
        user.setLastLogin(null);
        user.setCreatedAt(null);
        user.setUpdatedAt(null);
    }

    // 회원탈퇴 요청 시각 스탬프
    @Transactional
    public void requestQuitDateStamp(User user) {
        user.setRequestedQuitDate(LocalDateTime.now());
    }

    //회원탈퇴 대기
    @Transactional
    public void waitQuit(Long userIdx) {

        //구매한 시험 정보 삭제
        viewExamService.deleteByUserIdx(userIdx);

        //회원탈퇴 요청한 유저의 강의평가 삭제
        evaluatePostsService.deleteByUser(userIdx);

        //회원탈퇴 요청한 유저의 시험정보 삭제
        examPostsService.deleteByUser(userIdx);

        //유저 이용불가 처리
        disableUser(userService.loadUserFromUserIdx(userIdx));
    }

    // 회원탈퇴 요청 후 30일 뒤 테이블에서 제거
    // 테스트환경 : 회원탈퇴 요청 후 1분 후 테이블에서 제거
    @Transactional
//    @Scheduled(cron = "0 0 0 * * *")
    @Scheduled(cron = "0 * * * * *")
    public void deleteRequestQuitUserAfter30Days() {

//        LocalDateTime targetTime = LocalDateTime.now().minusDays(30);
        LocalDateTime targetTime = LocalDateTime.now().minusMinutes(1);

        List<User> targetUser = userRepository.findByRequestedQuitDateBefore(targetTime);
        List<UserIsolation> targetUserIsolation = userIsolationRepository.findByRequestedQuitDateBefore(targetTime);

        if (targetUser.size() > 0) {
            for (int i = 0; i < targetUser.toArray().length; i++) {
                userRepository.deleteById(targetUser.get(i).getId());
            }
        }

        for (int i = 0; i < targetUserIsolation.toArray().length; i++) {
            userIsolationRepository.deleteById(targetUserIsolation.get(i).getId());
        }
    }
}
