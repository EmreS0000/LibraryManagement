import React, { useState } from 'react';
import {
    Container, Box, Typography, TextField, Button, Alert, Grid, Paper,
    InputAdornment, IconButton, Fade, CircularProgress, Link as MuiLink, Stepper, Step, StepLabel
} from '@mui/material';
import { useNavigate, Link } from 'react-router-dom';
import { AuthService } from '../services/AuthService';
import {
    Visibility, VisibilityOff, Email, Lock, Person, Phone, LibraryBooks, CheckCircle
} from '@mui/icons-material';

interface RegisterData {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    password: string;
    confirmPassword?: string;
}

const Register: React.FC = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState<RegisterData>({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: '',
    });
    const [errors, setErrors] = useState<Partial<RegisterData>>({});
    const [apiError, setApiError] = useState('');
    const [loading, setLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [success, setSuccess] = useState(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name as keyof RegisterData]) {
            setErrors(prev => ({ ...prev, [name]: '' }));
        }
    };

    const validate = () => {
        const newErrors: Partial<RegisterData> = {};
        let isValid = true;

        if (!formData.firstName.trim()) {
            newErrors.firstName = 'First Name is required';
            isValid = false;
        }
        if (!formData.lastName.trim()) {
            newErrors.lastName = 'Last Name is required';
            isValid = false;
        }
        if (!formData.email.trim()) {
            newErrors.email = 'Email is required';
            isValid = false;
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            newErrors.email = 'Email is not valid';
            isValid = false;
        }
        if (!formData.phone.trim()) {
            newErrors.phone = 'Phone number is required';
            isValid = false;
        }
        if (!formData.password.trim()) {
            newErrors.password = 'Password is required';
            isValid = false;
        } else if (formData.password.length < 6) {
            newErrors.password = 'Password must be at least 6 characters';
            isValid = false;
        }
        if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Passwords do not match';
            isValid = false;
        }

        setErrors(newErrors);
        return isValid;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setApiError('');

        if (!validate()) return;

        setLoading(true);
        try {
            const { confirmPassword, ...registerData } = formData;
            console.log('Sending registration data:', registerData);
            const response = await AuthService.register(registerData);
            console.log('Registration response:', response);
            setSuccess(true);
            setTimeout(() => {
                navigate('/login');
            }, 2000);
        } catch (err: any) {
            console.error('Registration error:', err);
            console.error('Error response:', err.response);
            
            // Handle validation errors
            if (err.response?.status === 400) {
                const errorData = err.response?.data;
                console.error('Validation errors:', errorData);
                
                // If it's a validation error map (field: message)
                if (typeof errorData === 'object' && !errorData.message) {
                    const fieldErrors = Object.entries(errorData).map(([field, msg]) => `${field}: ${msg}`).join(', ');
                    setApiError(`Validation failed: ${fieldErrors}`);
                } else {
                    setApiError(errorData?.message || 'Validation failed. Please check your input.');
                }
            } else {
                const errorMessage = err.response?.data?.message || err.message || 'Registration failed. Email might be in use.';
                setApiError(errorMessage);
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                position: 'relative',
                overflow: 'hidden',
                py: 4,
            }}
        >
            {/* Animated background circles */}
            <Box
                sx={{
                    position: 'absolute',
                    width: '600px',
                    height: '600px',
                    borderRadius: '50%',
                    background: 'rgba(255,255,255,0.1)',
                    top: '-300px',
                    left: '-300px',
                    animation: 'float 7s ease-in-out infinite',
                    '@keyframes float': {
                        '0%, 100%': { transform: 'translateY(0px) rotate(0deg)' },
                        '50%': { transform: 'translateY(30px) rotate(180deg)' },
                    },
                }}
            />
            <Box
                sx={{
                    position: 'absolute',
                    width: '400px',
                    height: '400px',
                    borderRadius: '50%',
                    background: 'rgba(255,255,255,0.08)',
                    bottom: '-150px',
                    right: '-150px',
                    animation: 'float 9s ease-in-out infinite',
                }}
            />

            <Container component="main" maxWidth="md" data-testid="register-page">
                <Fade in={true} timeout={800}>
                    <Paper
                        elevation={24}
                        sx={{
                            p: { xs: 3, sm: 5 },
                            borderRadius: 4,
                            background: 'rgba(255,255,255,0.95)',
                            backdropFilter: 'blur(10px)',
                            boxShadow: '0 8px 32px 0 rgba(31, 38, 135, 0.37)',
                        }}
                    >
                        {success ? (
                            <Box sx={{ textAlign: 'center', py: 5 }}>
                                <CheckCircle
                                    sx={{
                                        fontSize: 100,
                                        color: '#4caf50',
                                        animation: 'scaleIn 0.5s ease',
                                        '@keyframes scaleIn': {
                                            '0%': { transform: 'scale(0)' },
                                            '100%': { transform: 'scale(1)' },
                                        },
                                    }}
                                />
                                <Typography variant="h4" color="primary" fontWeight="bold" mt={2}>
                                    Registration Successful!
                                </Typography>
                                <Typography variant="body1" color="text.secondary" mt={1}>
                                    Redirecting to login...
                                </Typography>
                            </Box>
                        ) : (
                            <>
                                <Box sx={{ textAlign: 'center', mb: 4 }}>
                                    <Box
                                        sx={{
                                            display: 'inline-flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            width: 70,
                                            height: 70,
                                            borderRadius: '50%',
                                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                            mb: 2,
                                            boxShadow: '0 4px 20px rgba(102, 126, 234, 0.4)',
                                        }}
                                    >
                                        <LibraryBooks sx={{ fontSize: 36, color: 'white' }} />
                                    </Box>
                                    <Typography
                                        component="h1"
                                        variant="h4"
                                        fontWeight="bold"
                                        sx={{
                                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                            backgroundClip: 'text',
                                            WebkitBackgroundClip: 'text',
                                            WebkitTextFillColor: 'transparent',
                                            mb: 1,
                                        }}
                                        data-testid="register-header"
                                    >
                                        Create Account
                                    </Typography>
                                    <Typography variant="body1" color="text.secondary">
                                        Join our library community today
                                    </Typography>
                                </Box>

                                {apiError && (
                                    <Fade in={true}>
                                        <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }} data-testid="register-error">
                                            {apiError}
                                        </Alert>
                                    </Fade>
                                )}

                                <Box component="form" onSubmit={handleSubmit} sx={{ width: '100%' }}>
                                    <Grid container spacing={2}>
                                        <Grid item xs={12} sm={6}>
                                            <TextField
                                                name="firstName"
                                                required
                                                fullWidth
                                                label="First Name"
                                                value={formData.firstName}
                                                onChange={handleChange}
                                                error={!!errors.firstName}
                                                helperText={errors.firstName}
                                                inputProps={{ "data-testid": "firstname-input" }}
                                                InputProps={{
                                                    startAdornment: (
                                                        <InputAdornment position="start">
                                                            <Person color="action" />
                                                        </InputAdornment>
                                                    ),
                                                }}
                                                sx={{
                                                    '& .MuiOutlinedInput-root': {
                                                        borderRadius: 2,
                                                        '&:hover fieldset': {
                                                            borderColor: '#667eea',
                                                        },
                                                    },
                                                }}
                                            />
                                        </Grid>
                                        <Grid item xs={12} sm={6}>
                                            <TextField
                                                name="lastName"
                                                required
                                                fullWidth
                                                label="Last Name"
                                                value={formData.lastName}
                                                onChange={handleChange}
                                                error={!!errors.lastName}
                                                helperText={errors.lastName}
                                                inputProps={{ "data-testid": "lastname-input" }}
                                                InputProps={{
                                                    startAdornment: (
                                                        <InputAdornment position="start">
                                                            <Person color="action" />
                                                        </InputAdornment>
                                                    ),
                                                }}
                                                sx={{
                                                    '& .MuiOutlinedInput-root': {
                                                        borderRadius: 2,
                                                        '&:hover fieldset': {
                                                            borderColor: '#667eea',
                                                        },
                                                    },
                                                }}
                                            />
                                        </Grid>
                                        <Grid item xs={12}>
                                            <TextField
                                                required
                                                fullWidth
                                                label="Email Address"
                                                name="email"
                                                type="email"
                                                value={formData.email}
                                                onChange={handleChange}
                                                error={!!errors.email}
                                                helperText={errors.email}
                                                inputProps={{ "data-testid": "email-input" }}
                                                InputProps={{
                                                    startAdornment: (
                                                        <InputAdornment position="start">
                                                            <Email color="action" />
                                                        </InputAdornment>
                                                    ),
                                                }}
                                                sx={{
                                                    '& .MuiOutlinedInput-root': {
                                                        borderRadius: 2,
                                                        '&:hover fieldset': {
                                                            borderColor: '#667eea',
                                                        },
                                                    },
                                                }}
                                            />
                                        </Grid>
                                        <Grid item xs={12}>
                                            <TextField
                                                required
                                                fullWidth
                                                label="Phone Number"
                                                name="phone"
                                                value={formData.phone}
                                                onChange={handleChange}
                                                error={!!errors.phone}
                                                helperText={errors.phone}
                                                inputProps={{ "data-testid": "phone-input" }}
                                                InputProps={{
                                                    startAdornment: (
                                                        <InputAdornment position="start">
                                                            <Phone color="action" />
                                                        </InputAdornment>
                                                    ),
                                                }}
                                                sx={{
                                                    '& .MuiOutlinedInput-root': {
                                                        borderRadius: 2,
                                                        '&:hover fieldset': {
                                                            borderColor: '#667eea',
                                                        },
                                                    },
                                                }}
                                            />
                                        </Grid>
                                        <Grid item xs={12}>
                                            <TextField
                                                required
                                                fullWidth
                                                label="Password"
                                                name="password"
                                                type={showPassword ? 'text' : 'password'}
                                                value={formData.password}
                                                onChange={handleChange}
                                                error={!!errors.password}
                                                helperText={errors.password}
                                                inputProps={{ "data-testid": "password-input" }}
                                                InputProps={{
                                                    startAdornment: (
                                                        <InputAdornment position="start">
                                                            <Lock color="action" />
                                                        </InputAdornment>
                                                    ),
                                                    endAdornment: (
                                                        <InputAdornment position="end">
                                                            <IconButton
                                                                onClick={() => setShowPassword(!showPassword)}
                                                                edge="end"
                                                            >
                                                                {showPassword ? <VisibilityOff /> : <Visibility />}
                                                            </IconButton>
                                                        </InputAdornment>
                                                    ),
                                                }}
                                                sx={{
                                                    '& .MuiOutlinedInput-root': {
                                                        borderRadius: 2,
                                                        '&:hover fieldset': {
                                                            borderColor: '#667eea',
                                                        },
                                                    },
                                                }}
                                            />
                                        </Grid>
                                        <Grid item xs={12}>
                                            <TextField
                                                required
                                                fullWidth
                                                label="Confirm Password"
                                                name="confirmPassword"
                                                type={showPassword ? 'text' : 'password'}
                                                value={formData.confirmPassword}
                                                onChange={handleChange}
                                                error={!!errors.confirmPassword}
                                                helperText={errors.confirmPassword}
                                                InputProps={{
                                                    startAdornment: (
                                                        <InputAdornment position="start">
                                                            <Lock color="action" />
                                                        </InputAdornment>
                                                    ),
                                                }}
                                                sx={{
                                                    '& .MuiOutlinedInput-root': {
                                                        borderRadius: 2,
                                                        '&:hover fieldset': {
                                                            borderColor: '#667eea',
                                                        },
                                                    },
                                                }}
                                            />
                                        </Grid>
                                    </Grid>
                                    <Button
                                        type="submit"
                                        fullWidth
                                        variant="contained"
                                        size="large"
                                        disabled={loading}
                                        sx={{
                                            mt: 3,
                                            mb: 2,
                                            py: 1.5,
                                            borderRadius: 2,
                                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                            fontSize: '1.1rem',
                                            fontWeight: 'bold',
                                            textTransform: 'none',
                                            boxShadow: '0 4px 15px rgba(102, 126, 234, 0.4)',
                                            '&:hover': {
                                                background: 'linear-gradient(135deg, #764ba2 0%, #667eea 100%)',
                                                boxShadow: '0 6px 20px rgba(102, 126, 234, 0.6)',
                                                transform: 'translateY(-2px)',
                                            },
                                            transition: 'all 0.3s ease',
                                        }}
                                        data-testid="register-button"
                                    >
                                        {loading ? (
                                            <CircularProgress size={24} sx={{ color: 'white' }} />
                                        ) : (
                                            'Create Account'
                                        )}
                                    </Button>
                                    <Box display="flex" justifyContent="center" mt={1}>
                                        <MuiLink
                                            component={Link}
                                            to="/login"
                                            variant="body1"
                                            sx={{
                                                color: '#667eea',
                                                textDecoration: 'none',
                                                fontWeight: 500,
                                                '&:hover': {
                                                    textDecoration: 'underline',
                                                },
                                            }}
                                        >
                                            Already have an account? Sign In
                                        </MuiLink>
                                    </Box>
                                </Box>
                            </>
                        )}
                    </Paper>
                </Fade>
            </Container>
        </Box>
    );
};

export default Register;
