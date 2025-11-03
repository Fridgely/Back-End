package soon.fridgely.global.security.jwt.provider;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.global.security.jwt.dto.response.TokenResponse;
import soon.fridgely.global.security.jwt.properties.JwtProperties;

import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Slf4j
public class JwtProvider implements TokenProvider {

    private static final String ROLE_CLAIM_KEY = "auth";

    private final Key key;
    private final long accessTokenExpirationTime;
    private final long refreshTokenExpirationTime;

    public JwtProvider(JwtProperties jwtProperties) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secretKey());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationTime = jwtProperties.accessTokenExpirationTime().toMillis();
        this.refreshTokenExpirationTime = jwtProperties.refreshTokenExpirationTime().toMillis();
    }

    @Override
    public TokenResponse generateAllToken(long memberId, String role) {
        String accessToken = generateAccessToken(memberId, MemberRole.from(role));
        String refreshToken = generateRefreshToken(memberId);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (SignatureException | MalformedJwtException e) {
            log.warn("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT, 만료된 JWT 입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT, 지원되지 않는 JWT 입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims is empty, 잘못된 JWT 입니다.");
        }
        return false;
    }

    @Override
    public Authentication getAuthentication(String token) {
        Claims claims = getClaimsFromToken(token);
        String memberId = claims.getSubject();

        MemberRole role = getRoleFromToken(claims);
        Collection<SimpleGrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority(role.name())
        );

        return new UsernamePasswordAuthenticationToken(memberId, "", authorities);
    }

    @Override
    public String getSubjectFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    private String generateAccessToken(long memberId, MemberRole role) {
        Date expirationDate = createExpirationDate(accessTokenExpirationTime);
        return Jwts.builder()
            .setSubject(String.valueOf(memberId))
            .claim(ROLE_CLAIM_KEY, role)
            .setExpiration(expirationDate)
            .setIssuedAt(new Date())
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    private String generateRefreshToken(long memberId) {
        Date expirationDate = createExpirationDate(refreshTokenExpirationTime);
        return Jwts.builder()
            .setExpiration(expirationDate)
            .setSubject(String.valueOf(memberId))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    private Date createExpirationDate(long expirationTime) {
        long currentTimeMillis = System.currentTimeMillis();
        return new Date(currentTimeMillis + expirationTime);
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private MemberRole getRoleFromToken(Claims claims) {
        return MemberRole.from(claims.get(ROLE_CLAIM_KEY, String.class));
    }

}