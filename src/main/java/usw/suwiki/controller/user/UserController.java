package usw.suwiki.controller.user;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import usw.suwiki.domain.refreshToken.RefreshToken;
import usw.suwiki.domain.user.User;
import usw.suwiki.domain.userIsolation.UserIsolation;
import usw.suwiki.dto.user.UserDto;
import usw.suwiki.dto.user.UserResponseDto;
import usw.suwiki.exception.AccountException;
import usw.suwiki.exception.ErrorType;
import usw.suwiki.jwt.JwtTokenProvider;
import usw.suwiki.jwt.JwtTokenResolver;
import usw.suwiki.jwt.JwtTokenValidator;
import usw.suwiki.repository.refreshToken.RefreshTokenRepository;
import usw.suwiki.service.emailAuth.EmailAuthService;
import usw.suwiki.service.emailBuild.BuildEmailAuthSuccessFormService;
import usw.suwiki.service.evaluation.EvaluatePostsService;
import usw.suwiki.service.exam.ExamPostsService;
import usw.suwiki.service.user.UserService;
import usw.suwiki.service.userIsolation.UserIsolationService;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Optional;


@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    //User 관련 서비스
    private final UserService userService;
    private final UserIsolationService userIsolationService;
    private final EmailAuthService emailAuthService;
    private final BuildEmailAuthSuccessFormService buildEmailAuthSuccessFormService;

    //JWT
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenValidator jwtTokenValidator;
    private final JwtTokenResolver jwtTokenResolver;
    private final RefreshTokenRepository refreshTokenRepository;
    
    //게시글 관련 서비스
    private final ExamPostsService examPostsService;
    private final EvaluatePostsService evaluatePostsService;

    //아이디 중복확인
    @PostMapping("check-id")
    public HashMap<String, Boolean> overlapId(@Valid @RequestBody UserDto.CheckIdForm checkId) {
        
        //반환객체 생성
        HashMap<String, Boolean> overlapLoginId = new HashMap<>();

        //아이디가 이미 존재하면
        if (userService.existId(checkId.getLoginId()).isPresent() || userIsolationService.existId(checkId.getLoginId()).isPresent()) {
            overlapLoginId.put("overlap", true);
            return overlapLoginId;
        }

        //아이디가 존재하지 않으면
        overlapLoginId.put("overlap", false);
        return overlapLoginId;
    }

    //이메일 중복 확인
    @PostMapping("check-email")
    public HashMap<String, Boolean> overlapEmail(@Valid @RequestBody UserDto.CheckEmailForm checkEmailForm) {

        //반환객체 생성
        HashMap<String, Boolean> overlapEmail = new HashMap<>();

        //이메일이 이미 존재하거나 블랙리스트 테이블에 있으면
        if (
                userService.existEmail(checkEmailForm.getEmail()).isPresent() ||
                userIsolationService.existEmail(checkEmailForm.getEmail()).isPresent() ||
                userService.existBlacklistEmail(checkEmailForm.getEmail())
            )
        {
            overlapEmail.put("overlap", true);
            return overlapEmail;
        }

        //이메일이 존재하지 않으면
        overlapEmail.put("overlap", false);
        return overlapEmail;
    }

    //회원가입 버튼 클릭 시 -> 유저 저장, 인증 이메일 발송
    @PostMapping("join")
    public HashMap<String, Boolean> join(@Valid @RequestBody UserDto.JoinForm joinForm) {

        //반환객체 생성
        HashMap<String, Boolean> joinSuccess = new HashMap<>();

        //블랙리스트 테이블에 존재하는 유저면 에러 터뜨리기
        if (userService.existBlacklistEmail(joinForm.getEmail())) throw new AccountException(ErrorType.USER_RESTRICTED);

        //회원가입 비즈니스 로직 호출
        userService.join(joinForm);

        joinSuccess.put("success", true);
        return joinSuccess;
    }

    //이메일 인증 링크 클릭 시
    @GetMapping("verify-email")
    public String ConfirmEmail(@RequestParam("token") String token) {

        String result = buildEmailAuthSuccessFormService.buildEmail();

        emailAuthService.confirmToken(token); //토큰 검증 --Error Code = 400

        emailAuthService.unRestricted(token); //제재 여부 false
        emailAuthService.userSetRole(token); //유저 권한 부여(USER)
        emailAuthService.userSetViewExamCount(token); //유저 조회한 시험 정보 갯수 0
        emailAuthService.userSetCreatedAt(token); //유저 생성 타임스탬프
        emailAuthService.userSetUpdatedAt(token); //유저 업데이트 타임스탬프

        return result;
    }

    //아이디 찾기 요청 시
    @PostMapping("find-id")
    public HashMap<String, Boolean> findId(@Valid @RequestBody UserDto.FindIdForm findIdForm) {

        //반환객체 생성
        HashMap<String, Boolean> joinSuccess = new HashMap<>();

        //아이디 찾기 요청 실패 시
        if (!userService.findId(findIdForm)) throw new AccountException(ErrorType.USER_NOT_EXISTS);

        joinSuccess.put("success", true);
        return joinSuccess;
    }

    //비밀번호 찾기 요청 시
    @PostMapping("find-pw")
    public HashMap<String, Boolean> findPw(@Valid @RequestBody UserDto.FindPasswordForm findPasswordForm) {

        //반환객체 생성
        HashMap<String, Boolean> findPwSuccess = new HashMap<>();

        //비밀번호 재설정 실패 시
        if (!userService.findPassword(findPasswordForm)) throw new AccountException(ErrorType.USER_NOT_EXISTS);

        findPwSuccess.put("success", true);
        return findPwSuccess;
    }

    //비밀번호 재설정 요청 시
    @PostMapping("reset-pw")
    public HashMap<String, Boolean> resetPw(@Valid @RequestBody UserDto.EditMyPasswordForm editMyPasswordForm, @RequestHeader String Authorization) {

        //토큰 검증
        jwtTokenValidator.validateAccessToken(Authorization);

        //토큰 검증 통과 시 반환 객체 생성
        HashMap<String, Boolean> findPwSuccess = new HashMap<>();

        //비밀번호 재설정 저장
        userService.editMyPassword(editMyPasswordForm, Authorization);

        findPwSuccess.put("success", true);
        return findPwSuccess;
    }

    //로그인 요청 시
    @PostMapping("login")
    public HashMap<String, String> login(@Valid @RequestBody UserDto.LoginForm loginForm) {

        HashMap<String, String> token = new HashMap<>();

        //유저 본 테이블에 존재하고, 제한된 유저가 아니라면 (isRestricted 는 유저 테이블에 Restricted 컬럼을 그대로 가져옴)
        if (userService.existId(loginForm.getLoginId()).isPresent() && !userService.isRestricted(loginForm.getLoginId())) {

            //아이디 비밀번호 검증
            userService.matchingLoginIdWithPassword(loginForm.getLoginId(), loginForm.getPassword());

            //유저 객체 생성
            Optional<User> optionalUser = userService.loadUserFromLoginId(loginForm.getLoginId());
            User user = userService.convertOptionalUserToDomainUser(optionalUser);

            //액세스 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(user);
            token.put("AccessToken", accessToken);

            //첫 로그인 대상자(리프레시 토큰이 DB에 없음)
            if (refreshTokenRepository.findByUserId(user.getId()).isEmpty()) {
                //리프레시 토큰 신규 생성
                String refreshToken = jwtTokenProvider.createRefreshToken();

                //리프레시토큰 저장
                refreshTokenRepository.save(
                        RefreshToken.builder()
                                .user(user)
                                .payload(refreshToken)
                                .build());

                //리프래시 토큰 반환객체에 담기
                token.put("RefreshToken", refreshToken);

                //마지막 로그인 일자 스탬프
                userService.setLastLogin(loginForm);
                
                //회원탈퇴 요청 시각 초기화
                userService.initQuitDateStamp(user);
                return token;
            }

            //첫 로그인이 아닌 대상자(이미 DB에 토큰이 있음)
            //리프레시 토큰 갱신
            String refreshToken = jwtTokenProvider.updateRefreshToken(user.getId());

            //리프래시 토큰 반환객체에 담기
            token.put("RefreshToken", refreshToken);

            //마지막 로그인 일자 스탬프
            userService.setLastLogin(loginForm);

            //회원탈퇴 요청 시각 초기화
            userService.initQuitDateStamp(user);
            return token;
        }

        //격리 테이블에 있으며 이메일 인증을 했으면 (대상 = 휴면계정, 회원탈퇴 요청 계정)
        if (userIsolationService.loadUserFromLoginId(loginForm.getLoginId()).isPresent() && !userService.isRestricted(loginForm.getLoginId())) {

            //격리 유저 Optional 객체 생성
            Optional<UserIsolation> optionalUserIsolation = userIsolationService.loadUserFromLoginId(loginForm.getLoginId());

            //격리 도메인 객체로 변환
            UserIsolation userIsolation = userIsolationService.convertOptionalUserToDomainUser(optionalUserIsolation);

            //본 테이블로 이동
            userService.moveUser(userIsolation);

            //본 도메인 객체 가져오기.
            User user = userService.loadUserFromUserIdx(userIsolation.getId());

            //격리 테이블 해당 유저 삭제
            userIsolationService.deleteIsolationUser(userIsolation.getId());

            //아이디 비밀번호 검증
            userService.matchingLoginIdWithPassword(loginForm.getLoginId(), loginForm.getPassword());

            //액세스 토큰, 리프레시 토큰 발급
            String accessToken = jwtTokenProvider.createAccessToken(user);

            //토큰 반환
            token.put("AccessToken", accessToken);

            //첫 로그인 대상자(리프레시 토큰이 DB에 없음)
            if (refreshTokenRepository.findByUserId(user.getId()).isEmpty()) {
                //리프레시 토큰 신규 생성
                String refreshToken = jwtTokenProvider.createRefreshToken();

                //리프레시토큰 저장
                refreshTokenRepository.save(
                        RefreshToken.builder()
                                .user(user)
                                .payload(refreshToken)
                                .build());

                //리프래시 토큰 반환객체에 담기
                token.put("RefreshToken", refreshToken);

                //마지막 로그인 일자 스탬프
                userService.setLastLogin(loginForm);

                //회원탈퇴 요청 시각 초기화
                userService.initQuitDateStamp(user);
                return token;
            }

            //첫 로그인이 아닌 대상자(이미 DB에 토큰이 있음)
            //리프레시 토큰 갱신
            String refreshToken = jwtTokenProvider.updateRefreshToken(user.getId());

            //리프레시토큰 반환 객체에 담기
            token.put("RefreshToken", refreshToken);

            //리프레시토큰 저장
            refreshTokenRepository.save(
                    RefreshToken.builder()
                            .user(user)
                            .payload(refreshToken)
                            .build());

            //마지막 로그인 일자 스탬프
            userService.setLastLogin(loginForm);

            //회원탈퇴 요청 시각 초기화
            userService.initQuitDateStamp(user);
            return token;
        }
        throw new AccountException(ErrorType.USER_AND_EMAIL_NOT_EXISTS_AND_AUTH);
    }

    @GetMapping("/my-page")
    public UserResponseDto.MyPageResponse myPage (@Valid @RequestHeader String Authorization) {

        //AccessToken 만료 확인
        jwtTokenValidator.validateAccessToken(Authorization);

        //토큰에 담긴 userIdx 가져오기
        Long userIdx = userService.loadUserIndexByAccessToken(Authorization);

        //토큰에 담김 loginId를 통해 레포지토리에 접근하여 User 불러오기
        User user = userService.loadUserFromUserIdx(userIdx);
        
        //반환
        return UserResponseDto.MyPageResponse.builder()
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .point(user.getPoint())
                .writtenLecture(user.getWrittenEvaluation())
                .writtenExam(user.getWrittenExam())
                .viewExam(user.getViewExamCount())
                .build();
    }

    @PostMapping("/refresh")
    public HashMap<String, String> tokenRefresh(@Valid @RequestHeader String Authorization) {

        //반환객체
        HashMap<String, String> token = new HashMap<>();

        //RefreshToken 유효기간 검증
        jwtTokenValidator.validateRefreshToken(Authorization);

        //RefreshToken DB에 담겨있는지 확인(임의로 만든 토큰이 아닌지 확인하자.)
        if (refreshTokenRepository.findByPayload(Authorization).isEmpty()) throw new AccountException(ErrorType.USER_RESTRICTED);

        //리프레시 토큰으로 유저 인덱스 뽑아오기
        Long userIdx = refreshTokenRepository.findByPayload(Authorization).get().getUser().getId();

        //해당 RefreshToken 으로 UserIndex 를 추출하여 객체 반환
        User user = userService.loadUserFromUserIdx(userIdx);

        //리프레시 토큰 갱신이 필요없으면
        if (!jwtTokenValidator.isNeedToUpdateRefreshToken(Authorization)) {
            //반환 객체에 담기
            token.put("AccessToken", jwtTokenProvider.createAccessToken(user));
            token.put("RefreshToken", Authorization);
        }

        //리프레시 토큰이 갱신 필요하면
        jwtTokenProvider.updateRefreshToken(userIdx);

        //RefreshToken 재생성
        String newRefreshToken = jwtTokenProvider.updateRefreshToken(user.getId());
        
        //반환객체에 담기
        token.put("RefreshToken", newRefreshToken);
        return token;
    }

    @PostMapping("quit")
    public HashMap<String, Boolean> userQuit(@Valid @RequestBody UserDto.UserQuitForm userQuitForm, @Valid @RequestHeader String Authorization) {

        //토큰 검증
        jwtTokenValidator.validateAccessToken(Authorization);

        //AccessToken 으로 요청 접근 권한이 있는지 확인
        if (jwtTokenResolver.getUserIsRestricted(Authorization)) throw new AccountException(ErrorType.USER_RESTRICTED);
        
        HashMap<String, Boolean> result = new HashMap<>();

        //아이디 비밀번호 검증 후 일치하지 않으면
        if (!userService.matchingLoginIdWithPassword(
                userQuitForm.getLoginId(), userQuitForm.getPassword())) throw new AccountException(ErrorType.USER_NOT_EXISTS);

        //아이디 비밀번호 검증 후 일치하면
        //해당하는 유저 가져오기
        Optional<User> optionalTheUserRequestedQuit = userService.loadUserFromLoginId(userQuitForm.getLoginId());

        //User Domain 객체로 변환
        User theUserRequestedQuit = userService.convertOptionalUserToDomainUser(optionalTheUserRequestedQuit);

        //회원탈퇴 요청 시각 스탬프
        userService.requestQuitDateStamp(theUserRequestedQuit);

        //해당 유저 아이디, 이메일 제외 모두 삭제
        userService.waitQuit(theUserRequestedQuit);

        result.put("success", true);

        return result;
    }

    @PostMapping("/report")
    public void report(@Valid @RequestBody UserDto.UserReportForm userReportForm, @Valid @RequestHeader String Authorization) {
        //토큰 검증
        jwtTokenValidator.validateAccessToken(Authorization);
        
        //AccessToken 으로 요청 접근 권한이 있는지 확인
        if (jwtTokenResolver.getUserIsRestricted(Authorization)) throw new AccountException(ErrorType.USER_RESTRICTED);
        
        //신고하기 비즈니스 로직 호출 --> 신고 테이블에 값 저장
        userService.reportUserPost(userReportForm);
    }
}

