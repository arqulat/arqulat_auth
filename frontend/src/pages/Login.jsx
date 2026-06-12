import React from 'react';
import AuthLayout from '../components/auth/AuthLayout';
import LoginForm from '../components/auth/LoginForm';
import '../styles/auth.css';

export default function Login() {
  return (
    <AuthLayout 
      title="Welcome Back"
      footerText="Don't have an account?"
      footerLink="/register"
      footerLinkText="Register here"
    >
      <LoginForm />
    </AuthLayout>
  );
}
