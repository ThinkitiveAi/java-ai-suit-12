import React, { useState } from 'react';
import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  IconButton,
  InputAdornment,
  Link,
  TextField,
  Typography,
  Alert,
  Fade,
  Paper,
  useTheme
} from '@mui/material';
import { Visibility, VisibilityOff, LocalHospital, ArrowBack } from '@mui/icons-material';
import { styled } from '@mui/material/styles';
import ProviderRegister from './ProviderRegister';
import ProviderDashboard from './ProviderDashboard';
import { providerAPI } from './services/api';

const RootContainer = styled('div')(({ theme }) => ({
  minHeight: '100vh',
  width: '100vw',
  display: 'flex',
  flexDirection: 'row',
  alignItems: 'stretch',
  justifyContent: 'center',
  overflow: 'hidden',
  [theme.breakpoints.down('md')]: {
    flexDirection: 'column',
    minHeight: '100vh',
  },
}));

const InfoSection = styled('div')(({ theme }) => ({
  flex: 1,
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
  background: 'rgba(0, 80, 160, 0.55)',
  color: '#fff',
  textAlign: 'center',
  zIndex: 1,
  position: 'relative',
  [theme.breakpoints.down('md')]: {
    minHeight: 180,
    padding: theme.spacing(4, 2, 2, 2),
  },
}));

const AnimatedFormPaper = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(5, 4),
  maxWidth: 420,
  margin: 'auto',
  borderRadius: theme.spacing(3),
  boxShadow: theme.shadows[8],
  background: 'rgba(255, 255, 255, 0.85)',
  backdropFilter: 'blur(10px) saturate(120%)',
  transition: 'box-shadow 0.3s, transform 0.3s',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  position: 'relative',
  zIndex: 2,
  [theme.breakpoints.down('md')]: {
    marginTop: theme.spacing(2),
    marginBottom: theme.spacing(2),
    maxWidth: '95vw',
    padding: theme.spacing(3, 1.5),
  },
}));

const initialState = {
  identifier: '', // Changed from email to identifier
  password: '',
  remember: false,
};

const initialErrors = {
  identifier: '', // Changed from email to identifier
  password: '',
  general: '',
};

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const phoneRegex = /^\+?[\d\s\-\(\)]{10,15}$/;
const minPasswordLength = 6;

const ProviderLogin: React.FC<{ onBackToIdentity?: () => void }> = ({ onBackToIdentity }) => {
  const [form, setForm] = useState(initialState);
  const [errors, setErrors] = useState(initialErrors);
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [showRegister, setShowRegister] = useState(false);
  const [showDashboard, setShowDashboard] = useState(false);
  const theme = useTheme();

  // Real-time validation
  const validate = (field: string, value: string) => {
    let error = '';
    if (field === 'identifier') {
      if (!value) error = 'Identifier is required.';
      else if (!emailRegex.test(value) && !phoneRegex.test(value)) error = 'Invalid identifier format.';
    }
    if (field === 'password') {
      if (!value) error = 'Password is required.';
      else if (value.length < minPasswordLength) error = `Password must be at least ${minPasswordLength} characters.`;
    }
    return error;
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    setErrors((prev) => ({
      ...prev,
      [name]: validate(name, type === 'checkbox' ? (checked ? 'true' : '') : value),
      general: '',
    }));
  };

  const handleShowPassword = () => setShowPassword((show) => !show);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validate all fields
    const identifierError = validate('identifier', form.identifier);
    const passwordError = validate('password', form.password);
    setErrors({
      identifier: identifierError,
      password: passwordError,
      general: '',
    });
    
    if (identifierError || passwordError) return;
    
    setLoading(true);
    setErrors(initialErrors);
    
    try {
      // Determine if identifier is email or phone
      const isEmail = emailRegex.test(form.identifier);
      const isPhone = phoneRegex.test(form.identifier);
      
      // Get device information
      const deviceInfo = {
        deviceType: 'web',
        deviceName: navigator.userAgent,
        appVersion: '1.0.0',
        operatingSystem: navigator.platform,
        browser: navigator.userAgent.includes('Chrome') ? 'Chrome' : 
                navigator.userAgent.includes('Firefox') ? 'Firefox' : 
                navigator.userAgent.includes('Safari') ? 'Safari' : 
                navigator.userAgent.includes('Edge') ? 'Edge' : 'Unknown'
      };
      
      // Prepare login data
      const loginData = {
        identifier: form.identifier,
        password: form.password,
        rememberMe: form.remember,
        deviceInfo: deviceInfo,
        deviceName: deviceInfo.deviceName,
        deviceType: deviceInfo.deviceType,
        appVersion: deviceInfo.appVersion,
        operatingSystem: deviceInfo.operatingSystem,
        emailIdentifier: isEmail,
        phoneIdentifier: isPhone,
        browser: deviceInfo.browser
      };
      
      console.log('🔐 Attempting login with:', loginData);
      
      // Call the API
      const response = await providerAPI.login(loginData);
      
      console.log('✅ Login successful:', response.data);
      
      // Show success message
      setSuccess(true);
      
      // Redirect to dashboard after success
      setTimeout(() => {
        setSuccess(false);
        setShowDashboard(true);
      }, 1200);
      
    } catch (error: any) {
      console.error('❌ Login failed:', error);
      setLoading(false);
      
      // Handle different types of errors
      if (error.response) {
        const errorData = error.response.data;
        console.log('🔍 Login error details:', errorData);
        
        if (errorData.errors) {
          // Map API validation errors to form errors
          const apiErrors: any = {};
          Object.keys(errorData.errors).forEach(key => {
            const formField = key === 'identifier' ? 'identifier' : key;
            apiErrors[formField] = errorData.errors[key];
          });
          setErrors(apiErrors);
        } else {
          // Handle other API errors
          const errorMessage = errorData.message || 
                             errorData.error || 
                             `Login failed (${error.response.status})`;
          setErrors((prev) => ({ ...prev, general: errorMessage }));
        }
      } else if (error.request) {
        // Network error
        setErrors((prev) => ({ ...prev, general: 'Network error. Please check your internet connection and try again.' }));
      } else {
        // Other error
        setErrors((prev) => ({ ...prev, general: 'An unexpected error occurred. Please try again.' }));
      }
    }
  };

  if (showDashboard) {
    return <ProviderDashboard />;
  }

  if (showRegister) {
    return (
      <RootContainer>
        <InfoSection>
          <Fade in timeout={900}>
            <Box>
              <LocalHospital sx={{ fontSize: 60, mb: 2, color: '#fff', filter: 'drop-shadow(0 2px 8px #1976d2)'}} />
              <Typography variant="h3" fontWeight={800} sx={{ letterSpacing: 1, mb: 1, color: '#fff' }}>
                MedCare Hospital
              </Typography>
              <Typography variant="h6" sx={{ color: '#e3f2fd', fontWeight: 400, mb: 2 }}>
                "Excellence in Healthcare"
              </Typography>
              <Typography variant="body1" sx={{ color: '#f3f8ff', maxWidth: 320, mx: 'auto', fontSize: 18 }}>
                Leading healthcare provider committed to delivering exceptional medical care with cutting-edge technology and compassionate service.
              </Typography>
              {onBackToIdentity && (
                <Button
                  variant="outlined"
                  color="inherit"
                  startIcon={<ArrowBack />}
                  onClick={onBackToIdentity}
                  sx={{ mt: 3, borderColor: 'rgba(255,255,255,0.3)', color: '#fff' }}
                >
                  Back to Selection
                </Button>
              )}
            </Box>
          </Fade>
        </InfoSection>
        <Box flex={1} display="flex" alignItems="center" justifyContent="center" sx={{ minHeight: '100vh', position: 'relative', zIndex: 2 }}>
          <ProviderRegister 
            onBackToLogin={() => setShowRegister(false)} 
            onRegistrationSuccess={() => setShowDashboard(true)}
          />
        </Box>
      </RootContainer>
    );
  }

  return (
    <RootContainer>
      <InfoSection>
        <Fade in timeout={900}>
          <Box>
            <LocalHospital sx={{ fontSize: 60, mb: 2, color: '#fff', filter: 'drop-shadow(0 2px 8px #0af)'}} />
            <Typography variant="h3" fontWeight={800} sx={{ letterSpacing: 1, mb: 1, color: '#fff' }}>
              MedCare Hospital
            </Typography>
            <Typography variant="h6" sx={{ color: '#e0f7fa', fontWeight: 400, mb: 2 }}>
              "Caring for Life, Every Day"
            </Typography>
            <Typography variant="body1" sx={{ color: '#e3f2fd', maxWidth: 320, mx: 'auto', fontSize: 18 }}>
              Welcome to MedCare Hospital's Patient Management System. Secure, seamless access for healthcare professionals.
            </Typography>
            {onBackToIdentity && (
              <Button
                variant="outlined"
                color="inherit"
                startIcon={<ArrowBack />}
                onClick={onBackToIdentity}
                sx={{ mt: 3, borderColor: 'rgba(255,255,255,0.3)', color: '#fff' }}
              >
                Back to Selection
              </Button>
            )}
          </Box>
        </Fade>
      </InfoSection>
      <Box
        flex={1}
        display="flex"
        alignItems="center"
        justifyContent="center"
        sx={{ minHeight: '100vh', position: 'relative', zIndex: 2 }}
      >
        <Fade in timeout={700}>
          <AnimatedFormPaper elevation={8}>
            <Typography variant="h5" align="center" gutterBottom fontWeight={700} sx={{ mb: 2, color: theme.palette.primary.main }}>
              Provider Login
            </Typography>
            <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1, width: '100%' }}>
              <TextField
                fullWidth
                label="Email or Phone"
                name="identifier"
                value={form.identifier}
                onChange={handleChange}
                error={!!errors.identifier}
                helperText={errors.identifier || "Enter your email address or phone number"}
                margin="normal"
                autoComplete="email"
                disabled={loading}
                autoFocus
                variant="outlined"
                placeholder="example@email.com or +1234567890"
                sx={{
                  background: 'rgba(255,255,255,0.95)',
                  borderRadius: 2,
                  boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
                  transition: 'box-shadow 0.2s',
                  '&:hover': { boxShadow: '0 4px 16px rgba(0,0,0,0.08)' },
                }}
              />
              <TextField
                fullWidth
                label="Password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                value={form.password}
                onChange={handleChange}
                error={!!errors.password}
                helperText={errors.password}
                margin="normal"
                autoComplete="current-password"
                disabled={loading}
                variant="outlined"
                sx={{
                  background: 'rgba(255,255,255,0.95)',
                  borderRadius: 2,
                  boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
                  transition: 'box-shadow 0.2s',
                  '&:hover': { boxShadow: '0 4px 16px rgba(0,0,0,0.08)' },
                }}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label="toggle password visibility"
                        onClick={handleShowPassword}
                        edge="end"
                        disabled={loading}
                        sx={{ color: theme.palette.primary.main }}
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
              <Box display="flex" alignItems="center" justifyContent="space-between" mt={1}>
                <FormControlLabel
                  control={
                    <Checkbox
                      name="remember"
                      checked={form.remember}
                      onChange={handleChange}
                      disabled={loading}
                      sx={{ p: 0.5, color: theme.palette.primary.main }}
                    />
                  }
                  label={<Typography variant="body2">Remember Me</Typography>}
                />
                <Link href="#" underline="hover" variant="body2" sx={{ ml: 1, color: theme.palette.primary.main }}>
                  Forgot Password?
                </Link>
              </Box>
              {errors.general && (
                <Alert severity="error" sx={{ mt: 2 }}>
                  {errors.general}
                </Alert>
              )}
              <Box mt={3} position="relative">
                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  color="primary"
                  size="large"
                  disabled={loading}
                  sx={{
                    py: 1.5,
                    fontWeight: 600,
                    fontSize: '1rem',
                    borderRadius: 2,
                    boxShadow: '0 2px 8px rgba(0,0,0,0.10)',
                    transition: 'transform 0.2s',
                    '&:active': { transform: 'scale(0.98)' },
                  }}
                >
                  {loading ? <CircularProgress size={26} color="inherit" /> : 'Login'}
                </Button>
                {success && (
                  <Fade in={success}>
                    <Alert severity="success" sx={{ mt: 2 }}>
                      Login successful! Redirecting...
                    </Alert>
                  </Fade>
                )}
                {/* Register Button */}
                <Box mt={3} textAlign="center">
                  <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>
                    Don't have an account?
                  </Typography>
                  <Button
                    variant="outlined"
                    color="primary"
                    size="medium"
                    sx={{ borderRadius: 2, fontWeight: 500, textTransform: 'none' }}
                    onClick={() => setShowRegister(true)}
                  >
                    Register here
                  </Button>
                </Box>
              </Box>
            </Box>
          </AnimatedFormPaper>
        </Fade>
      </Box>
    </RootContainer>
  );
};

export default ProviderLogin; 