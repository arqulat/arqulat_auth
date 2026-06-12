import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import axiosInstance from '../api/axiosConfig';
import '../styles/Auth.css';

const LocalLogin = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        
        try {
            await axiosInstance.post('/api/v1/auth/login', {
                email,
                password
            });
            // On success, execute dynamic redirect with fallback
            const target = sessionStorage.getItem('redirect_to') || 'https://arqulat.com';
            sessionStorage.removeItem('redirect_to');
            window.location.href = target;
        } catch (err) {
            if (err.response?.status === 400 && typeof err.response?.data === 'object' && !err.response?.data?.message) {
                const errorMessages = Object.values(err.response.data).join(' | ');
                setError(errorMessages);
            } else {
                setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
            }
        }
    };

    return (
        <div className="auth-container">
            <form className="auth-form" onSubmit={handleSubmit}>
                <h2 className="auth-title">Sign In</h2>
                
                {error && <div className="error-message">{error}</div>}
                
                <input
                    type="email"
                    className="input-field"
                    placeholder="Email Address"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                />
                
                <input
                    type="password"
                    className="input-field"
                    placeholder="Password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />
                
                <button type="submit" className="primary-btn">
                    Sign In
                </button>
                
                <Link to="/register" className="auth-link">
                    Don't have an account? Register here
                </Link>
                <Link to="/" className="auth-link">
                    Back to options
                </Link>
            </form>
        </div>
    );
};

export default LocalLogin;
