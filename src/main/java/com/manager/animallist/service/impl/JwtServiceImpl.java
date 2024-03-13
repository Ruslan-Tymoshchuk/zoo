package com.manager.animallist.service.impl;

import static java.lang.System.*;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static com.manager.animallist.payload.JWTMarkers.BEARER_TOKEN_TYPE;
import static  java.lang.String.format;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.manager.animallist.service.JwtService;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static io.jsonwebtoken.security.Keys.*;

@Component
public class JwtServiceImpl implements JwtService {

    public static final String JWT_FORMAT = "%s %s";
    public static final Key SECRET_KEY = secretKeyFor(SignatureAlgorithm.HS256);
    
    @Value("${jwt.cookies.valid.time}")
    private Integer jwtCookiesValidTime;
    private final List<String> tokenBlackList = new ArrayList<>();

    @Override
    public String extractUserEmail(String token) {
        return this.getClaim(token, Claims::getSubject);
    }

    @Override
    public String generateToken(String userEmail, Integer validTime) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userEmail)
                .setIssuedAt(new Date(currentTimeMillis()))
                .setExpiration(new Date(currentTimeMillis() + validTime))
                .signWith(SECRET_KEY)
                .compact();
    }

    @Override
    public boolean validate(String token, UserDetails userDetails) {
        String convertedToken = decode(token, UTF_8);
        final String username = extractUserEmail(convertedToken);
        return username.equals(userDetails.getUsername()) && !this.isTokenExpired(convertedToken);
    }

    @Override
    public String createJwtCookie(String tokenType, String token) {
        return ResponseCookie
                .from(tokenType, encode(format(JWT_FORMAT, BEARER_TOKEN_TYPE, token), UTF_8))
                .maxAge(jwtCookiesValidTime).httpOnly(true).build().toString();
    }

    @Override
    public boolean isTokenInBlackList(String token) {
        return tokenBlackList.contains(token);
    }
    
    @Override
    public void addTokenToBlacklist(String token) {
        tokenBlackList.add(token);
    }
    
    private <T> T getClaim(String token, Function<Claims, T> clamsResolver) {
        final Claims claims = getAllClaims(token);
        return clamsResolver.apply(claims);
    }

    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDate(token);
        return expiration.before(new Date());
    }
    
    private Date getExpirationDate(String token) {
        return this.getClaim(token, Claims::getExpiration);
    }
}
