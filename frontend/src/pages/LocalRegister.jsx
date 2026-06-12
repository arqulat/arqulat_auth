import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axiosInstance from '../api/axiosConfig';
import '../styles/Auth.css';

const LocalRegister = () => {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        
        try {
            await axiosInstance.post('/api/v1/auth/register', {
                name,
                email,
                password
            });
            // Redirect to login after successful registration
            navigate('/login');
        } catch (err) {
            if (err.response?.status === 400 && typeof err.response?.data === 'object' && !err.response?.data?.message) {
                const errorMessages = Object.values(err.response.data).join(' | ');
                setError(errorMessages);
            } else {
                setError(err.response?.data?.message || 'Registration failed. Please try again.');
            }
        }
    };

    return (
        <div className="auth-container">
            <form className="auth-form" onSubmit={handleSubmit}>
                <h2 className="auth-title">Register</h2>
                
                {error && <div className="error-message">{error}</div>}
                
                <input
                    type="text"
                    className="input-field"
                    placeholder="Full Name (Optional)"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                />
                
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
                    Register
                </button>
                
                <Link to="/login" className="auth-link">
                    Already have an account? Sign in
                </Link>
            </form>
        </div>
    );
};

export default LocalRegister;
