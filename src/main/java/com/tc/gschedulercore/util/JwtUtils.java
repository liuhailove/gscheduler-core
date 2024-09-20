package com.tc.gschedulercore.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;

/**
 * jwt工具类
 *
 * @author honggang.liu
 */
public class JwtUtils {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class.getSimpleName());

    /**
     * 密钥
     */
    private static final String SECRET = "8bad2694-3aa8-4a29-a483-51c0e65cbee3";

    /**
     * 过期时间,单位为秒,6h
     **/
    private static final long EXPIRATION = 6 * 3600L;

    /**
     * 生成用户token,设置token超时时间
     */
    public static String createToken(String userName) {
        //过期时间
        Date expireDate = new Date(System.currentTimeMillis() + EXPIRATION * 1000);
        return JWT.create().withIssuer("auth0")
                .withClaim("userName", userName)
                .withAudience(userName)
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 校验token并解析token
     */
    public static Map<String, Claim> verifyToken(String token) {
        if (!StringUtils.hasLength(token)) {
            return null;
        }
        DecodedJWT jwt;
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
            jwt = verifier.verify(token);
        } catch (Exception e) {
            LOGGER.error("token解码异常,{}", e.getMessage());
            //解码异常则抛出异常
            return null;
        }
        return jwt.getClaims();
    }

    /**
     * 刷新token
     *
     * @param refreshToken 待刷新的token
     * @return token
     */
    public static String refreshToken(String refreshToken) {
        Date expiresAt = new Date(System.currentTimeMillis() + EXPIRATION * 1000);
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).withIssuer("auth0").build();
        DecodedJWT jwt = verifier.verify(refreshToken);
        return JWT.create()
                .withIssuer("auth0")
                .withClaim("userName", jwt.getClaim("userName").asString())
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(SECRET));
    }

}
