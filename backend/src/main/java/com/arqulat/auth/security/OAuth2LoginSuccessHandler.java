package com.arqulat.auth.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.arqulat.auth.model.User;
import com.arqulat.auth.repository.UserRepository;
import com.arqulat.auth.service.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	JwtService jwtService;

	@Autowired
	private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

	@Value("${app.cookie.domain}")
	private String cookieDomain;

	@Value("${app.frontend.url}")
	private String frontendUrl;

	@Value("${app.cookie.max-age}")
	private long cookieMaxAge;
	
	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		
		String email = oAuth2User.getAttribute("email");
		String googleId = oAuth2User.getAttribute("sub");
		String name = oAuth2User.getAttribute("name");
		
		log.info("OAuth2 authentication success for email: {}, googleId: {}", email, googleId);

		// Guard against missing required OAuth2 attributes
		if (email == null || googleId == null) {
			log.error("Missing required OAuth2 attributes: email={}, googleId={}", email, googleId);
			getRedirectStrategy().sendRedirect(request, response, frontendUrl + "?error=oauth2_missing_attributes");
			return;
		}

		// 1. Try to find by Google ID first (returning user via Google)
		// 2. Fall back to email lookup (user registered via email/password — link accounts)
		// 3. Otherwise create a new user
		User user = userRepository.findByGoogleId(googleId)
				.orElseGet(() -> userRepository.findByEmail(email)
						.map(existingUser -> {
							log.info("Linking Google ID to existing user account for email: {}", email);
							// Link the Google ID to the existing email/password account
							existingUser.setGoogleId(googleId);
							if (existingUser.getName() == null) existingUser.setName(name);
							return userRepository.save(existingUser);
						})
						.orElseGet(() -> {
							log.info("Creating new user account for email: {}", email);
							User newUser = new User();
							newUser.setEmail(email);
							newUser.setGoogleId(googleId);
							newUser.setName(name);
							return userRepository.save(newUser);
						}));
		
		AppUserDetails userDetails = new AppUserDetails(user);
		java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
		extraClaims.put("uid", user.getId().toString());
		String jwtToken = jwtService.generateToken(extraClaims, userDetails);
		
		ResponseCookie cookie = ResponseCookie.from("arqulat_session", jwtToken)
				.domain(cookieDomain)
				.path("/")
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.maxAge(cookieMaxAge) // 7 days by default
				.build();
		
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		
		// Invalidate the session to enforce stateless JWT authentication
		if (request.getSession(false) != null) {
			request.getSession(false).invalidate();
		}
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
		
		String targetUrl = frontendUrl;
		java.util.Optional<jakarta.servlet.http.Cookie> redirectCookie = com.arqulat.auth.util.CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME);
		if (redirectCookie.isPresent() && redirectCookie.get().getValue() != null && !redirectCookie.get().getValue().isBlank()) {
			try {
				String decodedUri = new String(java.util.Base64.getUrlDecoder().decode(redirectCookie.get().getValue()), java.nio.charset.StandardCharsets.UTF_8);
				// Basic security check: ensure it belongs to arqulat.com or localhost
				if (decodedUri.matches("https?://([a-zA-Z0-9-]+\\.)*arqulat\\.com.*") || decodedUri.startsWith("http://localhost:")) {
					targetUrl = decodedUri;
				}
			} catch (IllegalArgumentException e) {
				log.error("Failed to decode redirect_uri cookie", e);
			}
		}

		cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}

