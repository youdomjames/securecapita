package com.youdomjames.securecapita.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.youdomjames.securecapita.domain.UserPrincipal;
import com.youdomjames.securecapita.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {
    private static final String AUDIENCE = "CUSTOMER_MANAGEMENT_SERVICE";
    private static final String ISSUER = "YOUDOM_CONSULTING";
    private static final String AUTHORITIES = "authorities";
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 1_800_000;
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 432_000_000;
    @Value("${jwt.secret}")
    private String secret;

    private final UserService userService;

    public String createAccessToken(UserPrincipal principal){
        return JWT.create().withIssuer(ISSUER).withAudience(AUDIENCE)
                .withIssuedAt(new Date()).withSubject(principal.getUsername()).withArrayClaim(AUTHORITIES, getClaimsFromUser(principal))
                .withExpiresAt(new Date(currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(secret.getBytes()));
    }

    public String createRefreshToken(UserPrincipal principal){
        return JWT.create().withIssuer(ISSUER).withAudience(AUDIENCE)
                .withIssuedAt(new Date()).withSubject(principal.getUsername()) //add a logic to hide the username
                .withExpiresAt(new Date(currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(secret.getBytes()));
    }

    public String getSubject(String token, HttpServletRequest request){
        JWTVerifier verifier = getJWTVerifier();
        try {
            return verifier.verify(token).getSubject();
        }catch (TokenExpiredException exception){
            request.setAttribute("expiredMessage", exception.getMessage());
            throw exception;
        }catch (InvalidClaimException exception){
            request.setAttribute("invalidClaim", exception.getMessage());
            throw exception;
        }catch (Exception exception){
            log.error(exception.getMessage());
            throw exception;
        }
    }
    public List<GrantedAuthority> getAuthorities(String token){
        String[] claims = getClaimsFromToken(token);
        return stream(claims).map(SimpleGrantedAuthority::new).collect(toList());
    }

    public Authentication getAuthentication(String email, List<GrantedAuthority> authorities, HttpServletRequest request){
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userService.getUserByEmail(email), null, authorities);
        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return usernamePasswordAuthenticationToken;
    }

    public boolean isTokenValid(String email, String token){
        JWTVerifier verifier = getJWTVerifier();
        return StringUtils.isNotEmpty(email) && !isTokenExpired(verifier, token);
    }

    private boolean isTokenExpired(JWTVerifier verifier, String token) {
        Date expiration = verifier.verify(token).getExpiresAt();
        return expiration.before(new Date());
    }

    private String[] getClaimsFromUser(UserPrincipal principal) {
        return principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray(String[]::new);
    }

    private String[] getClaimsFromToken(String token) {
        JWTVerifier verifier = getJWTVerifier();
        return verifier.verify(token).getClaim(AUTHORITIES).asArray(String.class);
    }

    private JWTVerifier getJWTVerifier() {
        JWTVerifier jwtVerifier;
        try {
            Algorithm algorithm = Algorithm.HMAC512(secret);
            jwtVerifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        }catch (JWTVerificationException exception){
            throw new JWTVerificationException("Token cannot be verified");
        }
        return jwtVerifier;
    }
}
