package usw.suwiki.global.jwt;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import static io.jsonwebtoken.Jwts.parser;

@Component
@RequiredArgsConstructor
public class JwtTokenResolver {

    @Value("${spring.jwt.secret-key}")
    private String secretKey;

    //AccessToken 에서 userIdx 꺼내기
    public Long getId(String token) {

        // Object Type 으로 받는다. (Long 으로 강제 형변환이 안되어서 한번 거쳤다가 Long 으로 )
        // 기존에는 Integer 로 받았지만, Integer 범위를 넘어서는 값이 등장하면 런타임 에러가 발생한다.
        Object id = parser().setSigningKey(secretKey.getBytes()).parseClaimsJws(token).getBody().get("id");

        // ObjectType 을 Long 으로 변환
        return Long.valueOf(String.valueOf(id));
    }

    //AccessToken loginId 꺼내기
    public String getLoginId(String token) {
        return (String) parser().setSigningKey(secretKey.getBytes()).parseClaimsJws(token).getBody().get("loginId");
    }

    //AccessToken Role 꺼내기
    public String getUserRole(String token) {
        return (String) parser().setSigningKey(secretKey.getBytes()).parseClaimsJws(token).getBody().get("role");
    }

    //AccessToken Restricted 꺼내기
    public boolean getUserIsRestricted(String token) {
        return (boolean) parser().setSigningKey(secretKey.getBytes()).parseClaimsJws(token).getBody().get("restricted");
    }
}