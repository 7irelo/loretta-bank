import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import styles from './Register.module.css';

function Register() {
  const [id, setId] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [address, setAddress] = useState('');
  const [occupation, setOccupation] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch('http://localhost:3000/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ id, username, password, firstName, lastName, email, dateOfBirth, address, occupation, phone }),
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Registration failed');
      }

      localStorage.setItem('jwtToken', data.token);
      navigate('/');
    } catch (error) {
      setError(error.message);
    }
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>Register</h2>
      <form onSubmit={handleRegister} className={styles.form}>
        <div className={styles.inputGroup}>
          <label htmlFor="id" className={styles.label}>ID No.:</label>
          <input
            type="text"
            id="id"
            className={styles.input}
            value={id}
            onChange={(e) => setId(e.target.value)}
          />
        </div>
        <div className={styles.inputGroup}>
          <label htmlFor="firstName" className={styles.label}>First Name:</label>
          <input
            type="text"
            id="firstName"
            className={styles.input}
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
          />
        </div>
        <div className={styles.inputGroup}>
          <label htmlFor="lastName" className={styles.label}>Last Name:</label>
          <input
            type="text"
            id="lastName"
            className={styles.input}
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
          />
        </div>
        <div className={styles.inputGroup}>
          <label htmlFor="email" className={styles.label}>Email:</label>
          <input
            type="email"
            id="email"
            className={styles.input}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <div className={styles.inputGroup}>
          <label htmlFor="dateOfBirth" className={styles.label}>Date of Birth:</label>
          <input
            type="date"
            id="dateOfBirth"
            className={styles.input}
            value={dateOfBirth}
            onChange={(e) => setDateOfBirth(e.target.value)}
          />
        </div>
        <div className={styles.inputGroup}>
          <label htmlFor="address" className={styles.label}>Address:</label>
          <input
            type="text"
            id="address"
            className={styles.input}
            value={address}
            onChange={(e) => setAddress(e.target.value)}
          />
        </div>
        <div className={styles.inputGroup}>
          <label htmlFor="occupation" className={styles.label}>Occupation:</label>
          <input
            type="text"
            id="occupation"
            className={styles.input}
            value={occupation}
            onChange={(e) => setOccupation(e.target.value)}
          />
        </div>
        <div className={styles.inputGroup}>
          <label htmlFor="phone" className={styles.label}>Phone:</label>
          <input
            type="text"
            id="phone"
            className={styles.input}
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />
        </div>
        <div className={styles.inputGroup}>
          <label htmlFor="username" className={styles.label}>Username:</label>
          <input
            type="text"
            id="username"
            className={styles.input}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </div>
        <div className={styles.inputGroup}>
          <label htmlFor="password" className={styles.label}>Password:</label>
          <input
            type="password"
            id="password"
            className={styles.input}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <button type="submit" className={styles.button}>Register</button>
        {error && <div className={styles.error}>Error: {error}</div>}
      </form>
      <p className={styles.switch}>
        Already have an account? <Link to="/login" className={styles.link}>Login here</Link>
      </p>
    </div>
  );
}

export default Register;
