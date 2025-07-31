import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  TextField,
  Typography,
  MenuItem,
  InputAdornment,
  IconButton,
  Fade,
  Divider,
  useTheme,
  Paper,
  FormControlLabel,
  Checkbox,
  Alert,
  CircularProgress,
  Snackbar
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { Visibility, VisibilityOff, ArrowBack, Save } from '@mui/icons-material';
import { styled } from '@mui/material/styles';
import { providerAPI } from './services/api';

const specializations = [
  'cardiology',
  'dermatology',
  'endocrinology',
  'gastroenterology',
  'hematology',
  'infectious diseases',
  'nephrology',
  'neurology',
  'oncology',
  'pulmonology',
  'rheumatology',
  'family medicine',
  'internal medicine',
  'pediatrics',
  'obstetrics and gynecology',
  'psychiatry',
  'surgery',
  'orthopedics',
  'anesthesiology',
  'radiology',
  'pathology',
  'emergency medicine',
  'urology',
  'ophthalmology',
  'otolaryngology',
  'plastic surgery'
];

const initialState = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  specialization: '',
  license: '',
  experience: '',
  street: '',
  city: '',
  state: '',
  zip: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false,
};

const initialErrors = Object.fromEntries(Object.keys(initialState).map(k => [k, '']));

const FormPaper = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(5, 4),
  maxWidth: 650,
  width: '100%',
  borderRadius: theme.spacing(3),
  boxShadow: theme.shadows[8],
  background: 'rgba(255,255,255,0.95)',
  marginTop: theme.spacing(6),
  marginBottom: theme.spacing(6),
  maxHeight: '80vh',
  overflowY: 'auto',
  
  // Custom scrollbar styles
  scrollbarWidth: 'thin',
  scrollbarColor: `${theme.palette.primary.light} #e0e0e0`,
  '&::-webkit-scrollbar': {
    width: 10,
    borderRadius: theme.spacing(3),
    background: '#e0e0e0',
  },
  '&::-webkit-scrollbar-thumb': {
    background: theme.palette.primary.light,
    borderRadius: theme.spacing(3),
    minHeight: 40,
    border: `2px solid #e0e0e0`,
  },
  '&::-webkit-scrollbar-corner': {
    background: 'transparent',
  },
  [theme.breakpoints.down('md')]: {
    maxWidth: '98vw',
    padding: theme.spacing(3, 1.5),
    marginTop: theme.spacing(2),
    marginBottom: theme.spacing(2),
    maxHeight: '90vh',
  },
}));

const ProviderRegister: React.FC<{ onBackToLogin?: () => void; onRegistrationSuccess?: () => void }> = ({ onBackToLogin, onRegistrationSuccess }) => {
  const [form, setForm] = useState(initialState);
  const [errors, setErrors] = useState(initialErrors);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [apiSuccess, setApiSuccess] = useState<string | null>(null);
  const [rateLimitCountdown, setRateLimitCountdown] = useState<number | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const theme = useTheme();

  // Countdown effect for rate limit
  useEffect(() => {
    if (rateLimitCountdown && rateLimitCountdown > 0) {
      const timer = setTimeout(() => {
        setRateLimitCountdown(rateLimitCountdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (rateLimitCountdown === 0) {
      setRateLimitCountdown(null);
      setApiError(null);
    }
  }, [rateLimitCountdown]);

  // Simple validation (expand as needed)
  const validate = (field: string, value: string) => {
    switch (field) {
      case 'firstName':
      case 'lastName':
        if (!value) return 'Required';
        if (value.length < 2) return 'Min 2 characters';
        if (value.length > 50) return 'Max 50 characters';
        break;
      case 'email':
        if (!value) return 'Required';
        if (!/^\S+@\S+\.\S+$/.test(value)) return 'Invalid email';
        break;
      case 'phone':
        if (!value) return 'Required';
        if (!/^\+?\d{10,15}$/.test(value)) return 'Invalid phone';
        break;
      case 'specialization':
        if (!value) return 'Required';
        if (value.length < 3) return 'Min 3 characters';
        if (value.length > 100) return 'Max 100 characters';
        break;
      case 'license':
        if (!value) return 'Required';
        if (!/^[a-zA-Z0-9]+$/.test(value)) return 'Alphanumeric only';
        break;
      case 'experience':
        if (!value) return 'Required';
        if (isNaN(Number(value)) || Number(value) < 0 || Number(value) > 50) return '0-50 only';
        break;
      case 'street':
        if (!value) return 'Required';
        if (value.length > 200) return 'Max 200 characters';
        break;
      case 'city':
        if (!value) return 'Required';
        if (value.length > 100) return 'Max 100 characters';
        break;
      case 'state':
        if (!value) return 'Required';
        if (value.length > 50) return 'Max 50 characters';
        break;
      case 'zip':
        if (!value) return 'Required';
        if (!/^\w{3,10}$/.test(value)) return 'Invalid code';
        break;
      case 'password':
        if (!value) return 'Required';
        if (value.length < 6) return 'Min 6 characters';
        break;
      case 'confirmPassword':
        if (!value) return 'Required';
        if (value !== form.password) return 'Passwords do not match';
        break;
      case 'agreeToTerms':
        if (!value) return 'You must agree to the terms and conditions';
        break;
      default:
        break;
    }
    return '';
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setErrors((prev) => ({ ...prev, [name]: validate(name, value) }));
  };

  const handleBlur = (e: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setErrors((prev) => ({ ...prev, [name]: validate(name, value) }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Clear previous errors
    setErrors({});
    setApiError(null);
    
    // Validate all fields
    const newErrors: any = {};
    
    // Personal Information validation
    if (!form.firstName.trim()) newErrors.firstName = 'First name is required';
    else if (form.firstName.length < 2) newErrors.firstName = 'First name must be at least 2 characters';
    else if (form.firstName.length > 50) newErrors.firstName = 'First name must be less than 50 characters';
    
    if (!form.lastName.trim()) newErrors.lastName = 'Last name is required';
    else if (form.lastName.length < 2) newErrors.lastName = 'Last name must be at least 2 characters';
    else if (form.lastName.length > 50) newErrors.lastName = 'Last name must be less than 50 characters';
    
    if (!form.email.trim()) newErrors.email = 'Email is required';
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) newErrors.email = 'Please enter a valid email address';
    
    if (!form.phone.trim()) newErrors.phone = 'Phone number is required';
    else if (!/^\+?\d{10,15}$/.test(form.phone)) newErrors.phone = 'Please enter a valid phone number';
    
    // Professional Information validation
    if (!form.specialization.trim()) newErrors.specialization = 'Specialization is required';
    else if (form.specialization.length < 2) newErrors.specialization = 'Specialization must be at least 2 characters';
    else if (form.specialization.length > 100) newErrors.specialization = 'Specialization must be less than 100 characters';
    
    if (!form.license.trim()) newErrors.license = 'Medical license number is required';
    else if (!/^[a-zA-Z0-9]+$/.test(form.license)) newErrors.license = 'License number must be alphanumeric';
    else if (form.license.length < 5) newErrors.license = 'License number must be at least 5 characters';
    else if (form.license.length > 50) newErrors.license = 'License number must be less than 50 characters';
    
    if (!form.experience) newErrors.experience = 'Years of experience is required';
    else if (parseInt(form.experience.toString()) < 0) newErrors.experience = 'Years of experience cannot be negative';
    else if (parseInt(form.experience.toString()) > 50) newErrors.experience = 'Years of experience cannot exceed 50';
    
    // Clinic Address validation
    if (!form.street.trim()) newErrors.street = 'Street address is required';
    else if (form.street.length > 200) newErrors.street = 'Street address must be less than 200 characters';
    
    if (!form.city.trim()) newErrors.city = 'City is required';
    else if (form.city.length > 100) newErrors.city = 'City must be less than 100 characters';
    
    if (!form.state.trim()) newErrors.state = 'State/Province is required';
    else if (form.state.length > 50) newErrors.state = 'State/Province must be less than 50 characters';
    
    if (!form.zip.trim()) newErrors.zip = 'ZIP/Postal code is required';
    else if (!/^\w{3,10}$/.test(form.zip)) newErrors.zip = 'Please enter a valid ZIP/Postal code';
    
    // Account Security validation
    if (!form.password) newErrors.password = 'Password is required';
    else if (form.password.length < 6) newErrors.password = 'Password must be at least 6 characters long';
    else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{6,}$/.test(form.password)) newErrors.password = 'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character';
    
    if (!form.confirmPassword) newErrors.confirmPassword = 'Please confirm your password';
    else if (form.password !== form.confirmPassword) newErrors.confirmPassword = 'Passwords do not match';
    
    if (!form.agreeToTerms) newErrors.agreeToTerms = 'You must agree to the terms and conditions';
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    
    // If validation passes, proceed with API call
    setLoading(true);
    
    try {
      // Prepare data for API with proper validation and formatting
      const registrationData = {
        first_name: form.firstName.trim(),
        last_name: form.lastName.trim(),
        email: form.email.trim(),
        phone_number: form.phone.trim(),
        password: form.password,
        confirm_password: form.confirmPassword,
        specialization: form.specialization.trim(),
        license_number: form.license.trim(),
        years_of_experience: parseInt(form.experience.toString()) || 0,
        clinic_address: {
          street: form.street.trim(),
          city: form.city.trim(),
          state: form.state.trim(),
          zip: form.zip.trim()
        }
      };
      
      // Additional validation to ensure no empty strings are sent
      const validationErrors: any = {};
      
      if (!registrationData.first_name) validationErrors.firstName = 'First name is required';
      if (!registrationData.last_name) validationErrors.lastName = 'Last name is required';
      if (!registrationData.email) validationErrors.email = 'Email is required';
      if (!registrationData.phone_number) validationErrors.phone = 'Phone number is required';
      if (!registrationData.password) validationErrors.password = 'Password is required';
      if (!registrationData.confirm_password) validationErrors.confirmPassword = 'Password confirmation is required';
      if (!registrationData.specialization) validationErrors.specialization = 'Specialization is required';
      if (!registrationData.license_number) validationErrors.license = 'License number is required';
      if (registrationData.years_of_experience < 0) validationErrors.experience = 'Years of experience cannot be negative';
      if (!registrationData.clinic_address.street) validationErrors.street = 'Street address is required';
      if (!registrationData.clinic_address.city) validationErrors.city = 'City is required';
      if (!registrationData.clinic_address.state) validationErrors.state = 'State is required';
      if (!registrationData.clinic_address.zip) validationErrors.zip = 'ZIP code is required';
      
      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors);
        return;
      }
      
      console.log('📝 Submitting registration data:', registrationData);
      
      // Call the API
      const response = await providerAPI.register(registrationData);
      
      console.log('✅ Registration successful:', response.data);
      
      // Show success message and set success state
      setApiSuccess('Registration successful! Welcome to MedCare. Redirecting to dashboard...');
      setIsSuccess(true);
      
      // Clear any rate limit countdown
      setRateLimitCountdown(null);
      
      // Redirect to dashboard after a short delay
      setTimeout(() => {
        console.log('🔄 Redirecting to dashboard...');
        if (onRegistrationSuccess) {
          console.log('✅ Calling onRegistrationSuccess callback');
          onRegistrationSuccess();
        } else {
          console.log('⚠️ onRegistrationSuccess callback not provided');
        }
      }, 2000);
      
      // Reset form after successful registration
      setTimeout(() => {
        setForm({
          firstName: '',
          lastName: '',
          email: '',
          phone: '',
          specialization: '',
          license: '',
          experience: '',
          street: '',
          city: '',
          state: '',
          zip: '',
          password: '',
          confirmPassword: '',
          agreeToTerms: false
        });
        setErrors({});
        setApiSuccess(null);
      }, 3000);
      
    } catch (error: any) {
      console.error('❌ Registration failed:', error);
      
      // Handle different types of errors
      if (error.response) {
        // Server responded with error status
        const errorData = error.response.data;
        console.log('🔍 Error details:', errorData);
        
        if (errorData.errors) {
          // Map API validation errors to form errors
          const apiErrors: any = {};
          Object.keys(errorData.errors).forEach(key => {
            // Map API field names (snake_case) to form field names (camelCase)
            const formField = key === 'phone_number' ? 'phone' : 
                            key === 'license_number' ? 'license' : 
                            key === 'years_of_experience' ? 'experience' : 
                            key === 'first_name' ? 'firstName' :
                            key === 'last_name' ? 'lastName' :
                            key === 'confirm_password' ? 'confirmPassword' :
                            key === 'clinic_address' ? 'street' : key;
            apiErrors[formField] = errorData.errors[key];
          });
          setErrors(apiErrors);
        } else {
          // Handle other API errors (rate limit, server errors, etc.)
          const errorMessage = errorData.message || 
                             errorData.error || 
                             `Registration failed (${error.response.status})`;
          
          // Check for rate limit error
          if (errorData.error_code === 'RATE_LIMIT_EXCEEDED') {
            const retryAfter = errorData.retry_after_seconds || 0;
            const minutes = Math.ceil(retryAfter / 60);
            setApiError(`${errorMessage} Please try again in ${minutes} minutes.`);
            setRateLimitCountdown(retryAfter); // Set countdown in seconds
          } else {
            setApiError(errorMessage);
          }
        }
      } else if (error.request) {
        // Network error
        setApiError('Network error. Please check your internet connection and try again.');
      } else {
        // Other error
        setApiError('An unexpected error occurred. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const content = (
    <FormPaper elevation={8}>
      <Box
        display="flex"
        alignItems="center"
        sx={{
          position: 'sticky',
          top: 0,
          zIndex: 10,
          background: '#fff',
          borderTopLeftRadius: theme.spacing(3),
          borderTopRightRadius: theme.spacing(3),
          minHeight: 64,
          width: '100%',
          boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
        }}
      >
        <IconButton onClick={onBackToLogin} sx={{ mr: 1 }}>
          <ArrowBack />
        </IconButton>
        <Typography variant="h5" fontWeight={700} color={theme.palette.primary.main}>
          Provider Registration
        </Typography>
      </Box>
      <Box sx={{ px: 2, py: 2 }}>
        <form onSubmit={handleSubmit} autoComplete="off">
        {/* Personal Information */}
        <Typography variant="h6" fontWeight={600} mb={1} color="primary">
          Personal Information
        </Typography>
        <Grid container spacing={2} mb={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="First Name"
              name="firstName"
              value={form.firstName}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.firstName}
              helperText={errors.firstName}
              fullWidth
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Last Name"
              name="lastName"
              value={form.lastName}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.lastName}
              helperText={errors.lastName}
              fullWidth
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Email Address"
              name="email"
              value={form.email}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.email}
              helperText={errors.email}
              fullWidth
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Phone Number"
              name="phone"
              value={form.phone}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.phone}
              helperText={errors.phone}
              fullWidth
            />
          </Grid>
        </Grid>
        <Divider sx={{ my: 2, bgcolor: 'grey.200' }} />
        {/* Professional Information */}
        <Typography variant="h6" fontWeight={600} mb={1} color="primary">
          Professional Information
        </Typography>
        <Grid container spacing={2} mb={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              select
              label="Specialization"
              name="specialization"
              value={form.specialization}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.specialization}
              helperText={errors.specialization}
              fullWidth
            >
              {specializations.map((spec) => (
                <MenuItem key={spec} value={spec}>{spec}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Medical License Number"
              name="license"
              value={form.license}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.license}
              helperText={errors.license}
              fullWidth
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Years of Experience"
              name="experience"
              type="number"
              value={form.experience}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.experience}
              helperText={errors.experience}
              fullWidth
              inputProps={{ min: 0, max: 50 }}
            />
          </Grid>
        </Grid>
        <Divider sx={{ my: 2, bgcolor: 'grey.200' }} />
        {/* Clinic Address */}
        <Typography variant="h6" fontWeight={600} mb={1} color="primary">
          Clinic Address
        </Typography>
        <Grid container spacing={2} mb={2}>
          <Grid size={12}>
            <TextField
              label="Street Address"
              name="street"
              value={form.street}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.street}
              helperText={errors.street}
              fullWidth
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="City"
              name="city"
              value={form.city}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.city}
              helperText={errors.city}
              fullWidth
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              label="State/Province"
              name="state"
              value={form.state}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.state}
              helperText={errors.state}
              fullWidth
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 3 }}>
            <TextField
              label="ZIP/Postal Code"
              name="zip"
              value={form.zip}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.zip}
              helperText={errors.zip}
              fullWidth
            />
          </Grid>
        </Grid>
        <Divider sx={{ my: 2, bgcolor: 'grey.200' }} />
        {/* Account Security */}
        <Typography variant="h6" fontWeight={600} mb={1} color="primary">
          Account Security
        </Typography>
        <Grid container spacing={2} mb={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              value={form.password}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.password}
              helperText={errors.password}
              fullWidth
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setShowPassword((v) => !v)}>
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Confirm Password"
              name="confirmPassword"
              type={showConfirm ? 'text' : 'password'}
              value={form.confirmPassword}
              onChange={handleChange}
              onBlur={handleBlur}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword}
              fullWidth
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setShowConfirm((v) => !v)}>
                      {showConfirm ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
          </Grid>
        </Grid>
        <FormControlLabel
          control={
            <Checkbox
              checked={form.agreeToTerms}
              onChange={(e) => setForm((prev) => ({ ...prev, agreeToTerms: e.target.checked }))}
              name="agreeToTerms"
              color="primary"
            />
          }
          label="I agree to the terms and conditions"
          sx={{ mt: 1, mb: 2 }}
        />
        <Box textAlign="center" mt={2}>
          <Button
            type="submit"
            variant="contained"
            color="primary"
            size="large"
            sx={{ px: 6, py: 1.5, borderRadius: 2, fontWeight: 600 }}
            disabled={loading}
            startIcon={loading ? <CircularProgress size={20} /> : <Save />}
          >
            {loading ? 'Creating Account...' : 'Register'}
          </Button>
        </Box>
        <Box textAlign="center" mt={3}>
          <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>
            Already have an account?
          </Typography>
          <Button
            variant="outlined"
            color="primary"
            size="medium"
            sx={{ borderRadius: 2, fontWeight: 500, textTransform: 'none' }}
            onClick={onBackToLogin}
          >
            Login
          </Button>
        </Box>
      </form>
    </Box>
    </FormPaper>
  );

  if (onBackToLogin) {
    return <Fade in timeout={600}>{content}</Fade>;
  }

  // fallback: full page (should not be used in this app)
  return (
    <Box sx={{ 
      display: 'flex', 
      minHeight: '100vh',
      background: '#f5f5f5'
    }}>
      {/* Left Section - Hospital Information */}
      <Box sx={{
        flex: 1,
        display: { xs: 'none', md: 'flex' },
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'linear-gradient(135deg, #1976d2 0%, #1565c0 100%)',
        color: 'white',
        p: 4,
        position: 'relative',
        overflow: 'hidden'
      }}>
        {/* ... existing hospital info content ... */}
      </Box>

      {/* Right Section - Registration Form */}
      <Box sx={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        p: { xs: 2, sm: 4 },
        maxWidth: { xs: '100%', md: '600px' },
        mx: 'auto'
      }}>
        <Paper sx={{
          width: '100%',
          maxWidth: '500px',
          p: { xs: 2, sm: 4 },
          borderRadius: 3,
          boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
          position: 'relative',
          overflow: 'hidden'
        }}>
          {/* Sticky Header */}
          <Box sx={{
            position: 'sticky',
            top: 0,
            left: 0,
            right: 0,
            bgcolor: 'white',
            zIndex: 10,
            mb: 3,
            pb: 2,
            borderBottom: '1px solid #e0e0e0'
          }}>
            <Typography variant="h4" fontWeight={700} sx={{ color: '#333333', mb: 1 }}>
              Provider Registration
            </Typography>
            <Typography variant="body2" sx={{ color: '#666666' }}>
              Join our healthcare network and start managing your practice
            </Typography>
          </Box>

          {/* API Error/Success Messages */}
          {apiError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              <Box>
                <Typography variant="body2">
                  {apiError}
                </Typography>
                {rateLimitCountdown && rateLimitCountdown > 0 && (
                  <Typography variant="caption" sx={{ display: 'block', mt: 1, fontWeight: 600 }}>
                    ⏰ Retry available in: {Math.floor(rateLimitCountdown / 60)}:{(rateLimitCountdown % 60).toString().padStart(2, '0')}
                  </Typography>
                )}
              </Box>
            </Alert>
          )}
          
          {apiSuccess && (
            <Alert severity="success" sx={{ mb: 2 }}>
              <Box>
                <Typography variant="body2">
                  {apiSuccess}
                </Typography>
                {rateLimitCountdown && rateLimitCountdown > 0 && (
                  <Typography variant="caption" sx={{ display: 'block', mt: 1, fontWeight: 600 }}>
                    ⏰ Retry available in: {Math.floor(rateLimitCountdown / 60)}:{(rateLimitCountdown % 60).toString().padStart(2, '0')}
                  </Typography>
                )}
              </Box>
            </Alert>
          )}
          
          {isSuccess && (
            <Box sx={{
              position: 'fixed',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              bgcolor: 'rgba(76, 175, 80, 0.9)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              zIndex: 9999,
              color: 'white'
            }}>
              <Box sx={{ textAlign: 'center' }}>
                <Typography variant="h4" fontWeight={700} sx={{ mb: 2 }}>
                  🎉 Registration Successful!
                </Typography>
                <Typography variant="h6" sx={{ mb: 1 }}>
                  Welcome to MedCare
                </Typography>
                <Typography variant="body1">
                  Redirecting to dashboard...
                </Typography>
              </Box>
            </Box>
          )}

          {/* Registration Form */}
          <Box component="form" onSubmit={handleSubmit} sx={{
            maxHeight: '70vh',
            overflowY: 'auto',
            pr: 1,
            '&::-webkit-scrollbar': {
              width: '8px',
            },
            '&::-webkit-scrollbar-track': {
              background: '#f1f1f1',
              borderRadius: '4px',
            },
            '&::-webkit-scrollbar-thumb': {
              background: '#c1c1c1',
              borderRadius: '4px',
            },
            '&::-webkit-scrollbar-thumb:hover': {
              background: '#a8a8a8',
            },
          }}>
            {/* Personal Information */}
            <Typography variant="h6" fontWeight={600} mb={1} color="primary">
              Personal Information
            </Typography>
            <Grid container spacing={2} mb={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="First Name"
                  name="firstName"
                  value={form.firstName}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.firstName}
                  helperText={errors.firstName}
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Last Name"
                  name="lastName"
                  value={form.lastName}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.lastName}
                  helperText={errors.lastName}
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Email Address"
                  name="email"
                  value={form.email}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.email}
                  helperText={errors.email}
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Phone Number"
                  name="phone"
                  value={form.phone}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.phone}
                  helperText={errors.phone}
                  fullWidth
                />
              </Grid>
            </Grid>
            <Divider sx={{ my: 2, bgcolor: 'grey.200' }} />
            {/* Professional Information */}
            <Typography variant="h6" fontWeight={600} mb={1} color="primary">
              Professional Information
            </Typography>
            <Grid container spacing={2} mb={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  select
                  label="Specialization"
                  name="specialization"
                  value={form.specialization}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.specialization}
                  helperText={errors.specialization}
                  fullWidth
                >
                  {specializations.map((spec) => (
                    <MenuItem key={spec} value={spec}>{spec}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Medical License Number"
                  name="license"
                  value={form.license}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.license}
                  helperText={errors.license}
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Years of Experience"
                  name="experience"
                  type="number"
                  value={form.experience}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.experience}
                  helperText={errors.experience}
                  fullWidth
                  inputProps={{ min: 0, max: 50 }}
                />
              </Grid>
            </Grid>
            <Divider sx={{ my: 2, bgcolor: 'grey.200' }} />
            {/* Clinic Address */}
            <Typography variant="h6" fontWeight={600} mb={1} color="primary">
              Clinic Address
            </Typography>
            <Grid container spacing={2} mb={2}>
              <Grid size={12}>
                <TextField
                  label="Street Address"
                  name="street"
                  value={form.street}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.street}
                  helperText={errors.street}
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="City"
                  name="city"
                  value={form.city}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.city}
                  helperText={errors.city}
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 3 }}>
                <TextField
                  label="State/Province"
                  name="state"
                  value={form.state}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.state}
                  helperText={errors.state}
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 3 }}>
                <TextField
                  label="ZIP/Postal Code"
                  name="zip"
                  value={form.zip}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.zip}
                  helperText={errors.zip}
                  fullWidth
                />
              </Grid>
            </Grid>
            <Divider sx={{ my: 2, bgcolor: 'grey.200' }} />
            {/* Account Security */}
            <Typography variant="h6" fontWeight={600} mb={1} color="primary">
              Account Security
            </Typography>
            <Grid container spacing={2} mb={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  value={form.password}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.password}
                  helperText={errors.password}
                  fullWidth
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton onClick={() => setShowPassword((v) => !v)}>
                          {showPassword ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Confirm Password"
                  name="confirmPassword"
                  type={showConfirm ? 'text' : 'password'}
                  value={form.confirmPassword}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={!!errors.confirmPassword}
                  helperText={errors.confirmPassword}
                  fullWidth
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton onClick={() => setShowConfirm((v) => !v)}>
                          {showConfirm ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
            </Grid>
            <FormControlLabel
              control={
                <Checkbox
                  checked={form.agreeToTerms}
                  onChange={(e) => setForm((prev) => ({ ...prev, agreeToTerms: e.target.checked }))}
                  name="agreeToTerms"
                  color="primary"
                />
              }
              label="I agree to the terms and conditions"
              sx={{ mt: 1, mb: 2 }}
            />
            {/* Submit Button */}
            <Box sx={{ mt: 4, display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                disabled={loading || (rateLimitCountdown !== null && rateLimitCountdown > 0)}
                startIcon={loading ? <CircularProgress size={20} /> : <Save />}
                sx={{
                  py: 1.5,
                  borderRadius: 2,
                  fontSize: '1.1rem',
                  fontWeight: 600,
                  background: 'linear-gradient(135deg, #1976d2 0%, #1565c0 100%)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #1565c0 0%, #0d47a1 100%)',
                  },
                  '&:disabled': {
                    background: '#e0e0e0',
                  }
                }}
              >
                {loading ? 'Creating Account...' : 
                 (rateLimitCountdown !== null && rateLimitCountdown > 0) ? 'Rate Limited' : 'Create Account'}
              </Button>
              
              <Button
                variant="text"
                onClick={onBackToLogin}
                startIcon={<ArrowBack />}
                sx={{
                  color: '#666666',
                  '&:hover': {
                    color: '#333333',
                    bgcolor: '#f5f5f5'
                  }
                }}
              >
                Already have an account? Login
              </Button>
            </Box>
          </Box>
        </Paper>
      </Box>

      {/* Success Snackbar */}
      <Snackbar
        open={!!apiSuccess}
        autoHideDuration={4000}
        onClose={() => setApiSuccess(null)}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert onClose={() => setApiSuccess(null)} severity="success" sx={{ width: '100%' }}>
          {apiSuccess}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ProviderRegister; 