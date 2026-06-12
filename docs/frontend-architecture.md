# Accounts Frontend Architecture & Implementation Plan

This document outlines the architecture and folder structure for the central authentication service frontend (`accounts.arqulat.com`), built with Vite + React.

## Goal Description

Build a minimal, visually testable frontend routing to Google OAuth or Local Email/Password authentication. The app prioritizes a solid foundation over detailed styling, using basic semantic CSS classes for later theming by the design team. It ensures secure session management via HTTPOnly cookies using Axios, and automatically redirects authenticated users.

## User Review Required

> [!IMPORTANT]
> Please review this architecture document. Once you approve this plan, I will proceed to implement the codebase based on these specifications.

## Architecture Guidelines

1. **Routing Strategy**: Three primary pages (`/`, `/login`, `/register`).
2. **Minimal Styling**: All elements will use clean, semantic classes (e.g., `auth-container`, `input-field`). A single structural `Auth.css` will be used for basic alignment. 
3. **API Configuration**: Axios instance must strictly use `withCredentials: true` to handle `arqulat_session` HTTPOnly cookies.
4. **Global State**: `AuthContext.jsx` will check session validity on load via `/api/v1/user/me`. Valid sessions redirect to `https://loom.arqulat.com`.
5. **No Logout**: As this is purely an authentication gateway, no logout functionality will be implemented.

## Proposed Changes

### Folder Structure & Files

#### [NEW] [frontend-architecture.md](file:///d:/Ganesh-D/Projects/ARQULAT/arqulat_auth/docs/frontend-architecture.md)
This document serving as the architectural blueprint.

---

### Pages (`src/pages`)

#### [NEW] [MainLogin.jsx](file:///d:/Ganesh-D/Projects/ARQULAT/arqulat_auth/frontend/src/pages/MainLogin.jsx)
Mapped to `/`. The primary landing page featuring:
- A "Sign in with Google" button that redirects the browser directly to `http://localhost:8080/oauth2/authorization/google`.
- A link stating "Or sign in with email" that navigates to `/login`.

#### [NEW] [LocalLogin.jsx](file:///d:/Ganesh-D/Projects/ARQULAT/arqulat_auth/frontend/src/pages/LocalLogin.jsx)
Mapped to `/login`. A standard email/password login form containing full React state management and Axios API integration for local sign-in.

#### [NEW] [LocalRegister.jsx](file:///d:/Ganesh-D/Projects/ARQULAT/arqulat_auth/frontend/src/pages/LocalRegister.jsx)
Mapped to `/register`. A standard registration form capturing user details, featuring full React state management and Axios API integration for account creation.

---

### Global State (`src/context`)

#### [NEW] [AuthContext.jsx](file:///d:/Ganesh-D/Projects/ARQULAT/arqulat_auth/frontend/src/context/AuthContext.jsx)
Provides global authentication state. On initialization, it pings `/api/v1/user/me` to check for an existing `arqulat_session` cookie. If the user is authenticated, they are automatically redirected to `https://loom.arqulat.com`. 

---

### API Configuration (`src/api`)

#### [NEW] [axiosConfig.js](file:///d:/Ganesh-D/Projects/ARQULAT/arqulat_auth/frontend/src/api/axiosConfig.js)
The globally configured Axios instance. **Crucially**, it defines `withCredentials: true` to ensure all requests send and receive cookies from the backend.

---

### Styling (`src/styles`)

#### [NEW] [Auth.css](file:///d:/Ganesh-D/Projects/ARQULAT/arqulat_auth/frontend/src/styles/Auth.css)
A minimal, structural stylesheet providing basic flexbox centering, padding, margins, and simple borders. It heavily utilizes semantic class names (like `auth-container`, `google-btn`) intended for later overwriting by the design team.

---

### Routing (`src/`)

#### [MODIFY] [App.jsx](file:///d:/Ganesh-D/Projects/ARQULAT/arqulat_auth/frontend/src/App.jsx)
Configures the React Router to link the three main pages (`MainLogin`, `LocalLogin`, `LocalRegister`) and wraps the application with `AuthContext`.

## Verification Plan

### Manual Verification
1. **Routing**: Verify that `/`, `/login`, and `/register` render the correct components.
2. **Google OAuth**: Click "Sign in with Google" and verify it redirects properly to `http://localhost:8080/oauth2/authorization/google`.
3. **Session Check**: Verify that when a mock session/cookie is active, the application redirects to `https://loom.arqulat.com` on load.
4. **Styling Structure**: Inspect elements to ensure semantic classes (e.g. `auth-container`) are present and visually adequate for testing.
