package com.arqulat.auth.service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import com.arqulat.auth.repository.BlacklistedTokenRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {
	
	@Value("${application.security.jwt.secret-key}")
	private String secretKey;
	
	@Value("${application.security.jwt.expiration}")
	private long jwtExpiration;
	
	@Autowired
	private BlacklistedTokenRepository blacklistedTokenRepository;
	
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	public String generateToken(UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}
	
	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails){
		
		return Jwts.builder()
				.claims(extraClaims)
				.subject(userDetails.getUsername())
				.id(UUID.randomUUID().toString())
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + jwtExpiration))
				.signWith(getSignKey())
				.compact();
				
	}

	private SecretKey getSignKey() {
		byte[] keyBytes = Base64.getDecoder().decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
	
	public <T> T extractClaims(String jwtToken, Function<Claims, T> typeOfClaim) {
		
		Claims claims = Jwts.parser()
				.verifyWith(getSignKey())
				.build()
				.parseSignedClaims(jwtToken)
				.getPayload();
		
		return typeOfClaim.apply(claims);
	}
	
	public String extractUserName(String jwtToken) {
		
		return extractClaims(jwtToken, Claims::getSubject);
	}
	
	public String extractJti(String jwtToken) {
		return extractClaims(jwtToken, Claims::getId);
	}
	
	public Date extractExpiration(String jwtToken) {
		return extractClaims(jwtToken, Claims::getExpiration);
	}
	
	public boolean isTokenExpired(String jwtToken) {
		Date expireDate = extractClaims(jwtToken, Claims::getExpiration);
		return expireDate.before(new Date());
	}
	
	public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUserName(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token) && !isTokenBlacklisted(token);
    }
    
    public boolean isTokenBlacklisted(String jwtToken) {
        String jti = extractJti(jwtToken);
        if (jti == null) return false;
        
        // 1. Check Redis (fast path)
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey("blacklist:jti:" + jti))) {
            return true;
        }
        
        // 2. Fallback to DB (slow path)
        boolean isBlacklistedInDb = blacklistedTokenRepository.existsByJti(jti);
        if (isBlacklistedInDb) {
            // Backfill Redis
            Date expiration = extractExpiration(jwtToken);
            if (expiration != null) {
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    stringRedisTemplate.opsForValue().set("blacklist:jti:" + jti, "true", ttl, TimeUnit.MILLISECONDS);
                }
            }
            return true;
        }
        
        return false;
    }
}
