import React, { useState } from 'react';
import { authService } from '../../services/authService';
import '../../styles/auth.css';

export default function RegisterForm() {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // The legacy email auth forms are wrapped below so they can be easily deleted later

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.id]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!formData.firstName || !formData.lastName || !formData.email || !formData.password) {
      setError('Please fill in all fields.');
      return;
    }

    setLoading(true);
    try {
      const response = await authService.register(formData);
      console.log('Registration successful:', response);
      setSuccess('Registration successful! You can now log in.');
      // Optional: Clear form data
      setFormData({ firstName: '', lastName: '', email: '', password: '' });
    } catch (err) {
      setError(err.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      {error && <div className="auth-error-message">{error}</div>}
      {success && <div className="auth-success-message">{success}</div>}
      
      <div className="input-group">
        <label className="input-label" htmlFor="firstName">First Name</label>
        <input 
          id="firstName"
          type="text" 
          className="input-field" 
          placeholder="Enter your first name" 
          value={formData.firstName}
          onChange={handleChange}
        />
      </div>

      <div className="input-group">
        <label className="input-label" htmlFor="lastName">Last Name</label>
        <input 
          id="lastName"
          type="text" 
          className="input-field" 
          placeholder="Enter your last name" 
          value={formData.lastName}
          onChange={handleChange}
        />
      </div>

      <div className="input-group">
        <label className="input-label" htmlFor="email">Email</label>
        <input 
          id="email"
          type="email" 
          className="input-field" 
          placeholder="Enter your email" 
          value={formData.email}
          onChange={handleChange}
        />
      </div>

      <div className="input-group">
        <label className="input-label" htmlFor="password">Password</label>
        <input 
          id="password"
          type="password" 
          className="input-field" 
          placeholder="Create a password" 
          value={formData.password}
          onChange={handleChange}
        />
      </div>

      <button type="submit" className="auth-submit-btn" disabled={loading}>
        {loading ? 'Registering...' : 'Register'}
      </button>
    </form>
  );
}
