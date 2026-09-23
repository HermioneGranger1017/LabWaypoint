package com.itheima.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final Algorithm algorithm;

    public JwtUtil(@Value("${jwt.secret}") String signingKey) {
        if (signingKey == null || signingKey.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 characters");
        }
        this.algorithm = Algorithm.HMAC256(signingKey);
    }
    //接受业务数据
    public String genToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))
                .sign(algorithm);

    }
    //接收token,验证token,并返回业务数据
    public Map<String, Object> parseToken(String token) {
        return JWT.require(algorithm)
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }


}
