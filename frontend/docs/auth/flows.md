# Frontend Authentication Flows

This document details how the frontend interacts with the backend for authentication.

## 1. Login Flow
- **Component**: `LoginForm.jsx`
- **Route**: `/login`
- **Service**: `authService.login(email, password)`
- **Endpoint**: `POST /api/auth/login`
- **Description**: Collects user credentials, sends them to the backend, and handles the response. Currently displays an alert on success. 

## 2. Registration Flow
- **Component**: `RegisterForm.jsx`
- **Route**: `/register`
- **Service**: `authService.register(userData)`
- **Endpoint**: `POST /api/auth/register`
- **Description**: Collects first name, last name, email, and password. On success, prompts the user to switch to the login page.

## State Management
State is currently managed locally within the components using React's `useState` hook. Once login is fully integrated, a global state (e.g., Context API or Redux) may be introduced to manage the authenticated user session.
