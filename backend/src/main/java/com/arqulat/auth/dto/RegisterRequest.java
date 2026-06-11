package com.arqulat.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

	@NotBlank(message = "Email is required")
	@Email(message = "Must be a valid email address")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
	@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=*!_?-]).{8,100}$", 
			 message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
	private String password;

	// Optional — user can set their display name at registration
	private String name;
}
