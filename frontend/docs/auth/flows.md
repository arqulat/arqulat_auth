# Authentication Flows

The frontend handles two distinct authentication paradigms: OAuth2 and Local Database authentication.

## 1. Google OAuth2 Flow
- Triggered from `MainLogin.jsx`.
- **Pre-action**: The frontend captures the `?redirect_to=` parameter (if valid) and stores it in `sessionStorage` via `AuthContext.jsx`.
- **Action**: Clicking "Sign in with Google" triggers a hard browser redirect to `http://localhost:8080/oauth2/authorization/google`.
- **Why**: This delegates the entire OAuth handshake to the Spring Boot backend. The backend manages the redirect to Google, callback validation, and sets the HTTPOnly cookie. 
- **Return Trip**: The backend bounces the user back to the frontend. `AuthContext.jsx` runs, verifies the session, and seamlessly redirects the user to the saved `redirect_to` URL (or `https://arqulat.com`).

## 2. Local Login & Registration Flow
- Triggered from `LocalLogin.jsx` and `LocalRegister.jsx`.
- **Action**: User submits email and password. React state captures this data and passes it to Axios.
- **API Call**: `POST /api/v1/auth/login` or `/api/v1/auth/register`.
- **Result**: Upon `200 OK`, the backend has successfully set the `arqulat_session` cookie. The frontend dynamically redirects the user to the saved `redirect_to` target (or `https://arqulat.com`).

---

## Modifying the "Sign in with Email" Option

The link to access local Email/Password authentication is located in `src/pages/MainLogin.jsx`. 

### Where is it?
In `src/pages/MainLogin.jsx`, inside the `return` statement:
```jsx
<Link to="/login" className="auth-link">
    Or sign in with email
</Link>
```

### How to Remove/Disable it:
If you want to enforce Google OAuth only and disable local login:
1. Open `src/pages/MainLogin.jsx`.
2. Delete or comment out the `<Link>` block shown above.
3. (Optional) Remove the `/login` and `/register` routes from `src/App.jsx`.

### How to Enable it:
If the link is missing, ensure the code snippet above is present inside the `auth-form` div in `MainLogin.jsx` and the corresponding `/login` route is mapped to `<LocalLogin />` in `App.jsx`.
