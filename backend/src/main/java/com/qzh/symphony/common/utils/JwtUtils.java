package com.qzh.symphony.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {
    @Value("${symphony.jwt.secret}")
    private String SECRET;

    @Value("${symphony.jwt.expire}")
    private long EXPIRE;

    //生成token
    public String createToken(String userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRE * 1000);
        //HMAC256算法来签名
        Algorithm algorithm = Algorithm.HMAC256(SECRET.getBytes());
        return JWT.create()
                .withHeader(java.util.Collections.singletonMap("typ", "JWT"))
                .withSubject(userId)
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(algorithm);
    }

    //解析token
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    //用同一密钥解签
                    .setSigningKey(SECRET.getBytes())
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            System.err.println("JWT 解析异常: " + e.getMessage());
            return null;
        }
    }
}
