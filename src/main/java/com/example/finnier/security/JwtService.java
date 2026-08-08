package com.example.finnier.security;

import com.example.finnier.entity.User;
import com.example.finnier.enums.JWTTokenType;
import com.example.finnier.exception.TokenGenerationException;
import com.example.finnier.exception.TokenValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    @Value("${security.jwt.secret-key}")
    private String jwtSecretKey;
    @Value("${security.jwt.access-token-expiration-time}")
    private long accessTokenExpiration;
    @Value("${security.jwt.refresh-token-expiration-time}")
    private long refreshTokenExpiration;

    private final String ISSUER = "Finnier";

    public String generateAccessToken(User user){
        try{
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId",user.getUserId());
            claims.put("status",user.getStatus());
            claims.put("role", user.getRole().name());
            claims.put("tokenType", JWTTokenType.ACCESS_TOKEN);

            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(user.getEmail())
                    .setIssuer(ISSUER)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                    .compact();
        }catch (Exception e){
            throw new TokenGenerationException("Failed to generate access token" ,e);
        }
    }

    public String generateRefreshToken(User user){
        try{
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId",user.getUserId());
            claims.put("status",user.getStatus());
            claims.put("role", user.getRole().name());
            claims.put("tokenType", JWTTokenType.REFRESH_TOKEN);

            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(user.getEmail())
                    .setIssuer(ISSUER)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                    .compact();
        }catch (Exception e){
            throw new TokenGenerationException("Failed to generate refresh token" ,e);
        }
    }

    public Claims validateToken(String token){
        try{
            return parseClaims(token);
        }catch (ExpiredJwtException e){
            throw new TokenValidationException("Token has expired ");
        }catch (UnsupportedJwtException e){
            throw new TokenValidationException("Unsupported JWT token ");
        }catch (MalformedJwtException e) {
            throw new TokenValidationException("Malformed JWT token ");
        }catch (io.jsonwebtoken.security.SignatureException e){
            throw new TokenValidationException("Invalid JWT signature ");
        }catch (IllegalArgumentException e){
            throw new TokenValidationException("JWT token compact string is invalid ");
        }catch (IncorrectClaimException e) {
            throw new TokenValidationException("Invalid JWT claims");
        }
    }
    public String extractUserRoleFromToken(String token){
        Claims claims = validateToken(token);
        return claims.get("role", String.class);
    }

    public String extractEmailFromToken(String token){
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    public boolean isRefreshToken(String token){
        Claims claims = validateToken(token);
        return claims.get("tokenType", String.class).equals(JWTTokenType.REFRESH_TOKEN.name());
    }

    private SecretKey getSigningKey(){
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}
