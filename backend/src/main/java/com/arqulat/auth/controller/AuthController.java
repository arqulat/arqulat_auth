package com.arqulat.auth.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.arqulat.auth.dto.AuthResponse;
import com.arqulat.auth.dto.LoginRequest;
import com.arqulat.auth.dto.RegisterRequest;
import com.arqulat.auth.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class AuthController {

	@Autowired
	AuthService authService;

	/*
	@PostMapping("/api/v1/auth/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
	}

	@PostMapping("/api/v1/auth/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		return new ResponseEntity<>(authService.login(request, response), HttpStatus.OK);
	}
	*/
	
	@GetMapping("/api/v1/user/me")
	public ResponseEntity<AuthResponse> getCurrentUser(Principal principal) {
		return new ResponseEntity<>(authService.getCurrentUser(principal.getName()), HttpStatus.OK);
	}
	
	@PostMapping("/api/v1/user/logout")
	public ResponseEntity<String> logout(HttpServletResponse response) {
		authService.logout(response);
		return new ResponseEntity<>("Logged out successfully", HttpStatus.OK);
	}
}
