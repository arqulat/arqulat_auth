package com.arqulat.auth.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.arqulat.auth.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	@Autowired
	JwtService jwtService;

	@Autowired
	AppUserDetailsService appUserDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String jwtToken = null;
		String userName = null;

		// 1. Check for Cookies and hunt for our specific SSO cookie
		if (request.getCookies() != null) {
			Optional<Cookie> authCookie = Arrays.stream(request.getCookies())
					.filter(cookie -> "arqulat_session".equals(cookie.getName()))
					.findFirst();

			// 2. If the cookie exists, extract the JWT
			if (authCookie.isPresent()) {
				jwtToken = authCookie.get().getValue();
				try {
					userName = jwtService.extractUserName(jwtToken);
					log.debug("Successfully extracted username from JWT: {}", userName);
				} catch (Exception e) {
					log.warn("Failed to extract username from JWT: {}", e.getMessage());
					// Token is expired, malformed, or invalid.
					// We do nothing here; the user simply remains unauthenticated.
				}
			} else {
				log.trace("No arqulat_session cookie found in request");
			}
		}

		if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {

			AppUserDetails userDetails = appUserDetailsService.loadUserByUsername(userName);

			if (jwtService.isTokenValid(jwtToken, userDetails)) {
				UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());
				token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(token);
				log.debug("Authentication set in SecurityContext for user: {}", userName);
			} else {
				log.warn("JWT token is invalid for user: {}", userName);
			}
		}

		filterChain.doFilter(request, response);
	}

}
