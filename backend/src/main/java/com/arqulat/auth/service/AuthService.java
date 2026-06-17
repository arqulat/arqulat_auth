package com.arqulat.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.arqulat.auth.dto.AuthResponse;
import com.arqulat.auth.dto.LoginRequest;
import com.arqulat.auth.dto.RegisterRequest;
import com.arqulat.auth.exception.ResourceNotFoundException;
import com.arqulat.auth.exception.UserAlreadyExistsException;
import com.arqulat.auth.model.User;
import com.arqulat.auth.repository.UserRepository;
import com.arqulat.auth.security.AppUserDetails;

import java.util.Arrays;
import java.util.Date;
import com.arqulat.auth.model.BlacklistedToken;
import com.arqulat.auth.repository.BlacklistedTokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthService {

	@Value("${app.cookie.domain}")
	private String cookieDomain;

	@Value("${app.cookie.max-age}")
	private long cookieMaxAge;


	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;
	
	@Autowired
	AuthenticationManager authenticationManager;
	
	@Autowired
	JwtService jwtService;
	
	@Autowired
	BlacklistedTokenRepository blacklistedTokenRepository;
	
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new UserAlreadyExistsException("Email is already registered");
		}
		
		User user = new User();
		user.setEmail(request.getEmail());
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setName(request.getName()); // optional, may be null
		
		User savedUser = userRepository.save(user);
		
		return new AuthResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getName());
	}

	public AuthResponse login(LoginRequest request, HttpServletResponse response) {
		
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						request.getEmail(), 
						request.getPassword()
		));
		
		AppUserDetails userDetails = (AppUserDetails)authentication.getPrincipal();
		User user = userDetails.getUser();
		
		java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
		extraClaims.put("uid", user.getId().toString());
		String jwtToken = jwtService.generateToken(extraClaims, userDetails);
		
		setJwtCookie(response, jwtToken, cookieMaxAge);
		
		return new AuthResponse(user.getId(), user.getEmail(), user.getName());
	}

	public AuthResponse getCurrentUser(String email) {
		log.debug("Looking up user by email: {}", email);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> {
					log.warn("User not found for email: {}", email);
					return new ResourceNotFoundException("User not found");
				});
		
		return new AuthResponse(user.getId(), user.getEmail(), user.getName());
	}

	public void logout(HttpServletRequest request, HttpServletResponse response) {
		log.debug("Initiating logout process, extracting cookies");
		if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(cookie -> "arqulat_session".equals(cookie.getName()))
                    .findFirst()
                    .ifPresent(cookie -> {
                        String token = cookie.getValue();
                        try {
                            String jti = jwtService.extractJti(token);
                            Date expiration = jwtService.extractExpiration(token);
                            if (jti != null && expiration != null && !blacklistedTokenRepository.existsByJti(jti)) {
                                log.info("Blacklisting JWT with JTI: {}", jti);
                                BlacklistedToken blacklistedToken = new BlacklistedToken();
                                blacklistedToken.setJti(jti);
                                blacklistedToken.setExpiresAt(expiration);
                                blacklistedTokenRepository.save(blacklistedToken);
                            }
                        } catch (Exception e) {
                            log.warn("Error processing JWT during logout (might be expired already): {}", e.getMessage());
                        }
                    });
        }
		
		// Delete the cookie
		setJwtCookie(response, "", 0);
		
		// Ensure any lingering sessions (if created) are destroyed
		if (request.getSession(false) != null) {
			request.getSession(false).invalidate();
		}
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
	}
	
	private void setJwtCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
		ResponseCookie cookie = ResponseCookie.from("arqulat_session", token)
				.domain(cookieDomain) // The leading dot ensures all subdomains (like Loom) can read it
				.path("/")
				.httpOnly(true)         // Protects against XSS attacks 
				.secure(true)           // Forces HTTPS 
				.sameSite("Lax")        // Protects against CSRF
				.maxAge(maxAgeSeconds)
				.build();
				
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
