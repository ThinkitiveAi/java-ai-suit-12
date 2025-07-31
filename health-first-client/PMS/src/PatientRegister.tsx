import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Grid,
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
  Link,
  CircularProgress
} from '@mui/material';
import { Visibility, VisibilityOff, ArrowBack, Person, LocalHospital } from '@mui/icons-material';
import { styled } from '@mui/material/styles';
import { patientAPI } from './services/api';

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
  background: 'rgba(76, 175, 80, 0.55)', // Calming green overlay for patients
  color: '#fff',
  textAlign: 'center',
  zIndex: 1,
  position: 'relative',
  [theme.breakpoints.down('md')]: {
    minHeight: 180,
    padding: theme.spacing(4, 2, 2, 2),
  },
}));

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
  scrollbarColor: `${theme.palette.secondary.light} #e0e0e0`,
  '&::-webkit-scrollbar': {
    width: 10,
    borderRadius: theme.spacing(3),
    background: '#e0e0e0',
  },
  '&::-webkit-scrollbar-thumb': {
    background: theme.palette.secondary.light,
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

const genderOptions = [
  'Male',
  'Female', 
  'Other',
  'Prefer not to say'
];

const relationshipOptions = [
  'Spouse',
  'Parent',
  'Child',
  'Sibling',
  'Friend',
  'Other'
];

const initialState = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  dateOfBirth: '',
  gender: '',
  street: '',
  city: '',
  state: '',
  zip: '',
  emergencyName: '',
  emergencyRelationship: '',
  emergencyPhone: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false,
  marketingConsent: false,
  dataSharingConsent: false,
  // Insurance fields (optional)
  insuranceProvider: '',
  policyNumber: '',
  groupNumber: '',
  planName: '',
  memberId: '',
  // Medical history (optional)
  medicalHistory: '',
};

const initialErrors = Object.fromEntries(Object.keys(initialState).map(k => [k, '']));

const PatientRegister: React.FC<{ onBackToLogin: () => void; onRegistrationSuccess?: () => void }> = ({ onBackToLogin, onRegistrationSuccess }) => {
  const [form, setForm] = useState(initialState);
  const [errors, setErrors] = useState(initialErrors);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
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

  // Validation functions
  const validate = (field: string, value: string | boolean) => {
    switch (field) {
      case 'firstName':
      case 'lastName':
        if (!value) return 'Required';
        if (typeof value === 'string' && value.length < 2) return 'Min 2 characters';
        if (typeof value === 'string' && value.length > 50) return 'Max 50 characters';
        break;
      case 'email':
        if (!value) return 'Required';
        if (typeof value === 'string' && !/^\S+@\S+\.\S+$/.test(value)) return 'Invalid email format';
        break;
      case 'phone':
        if (!value) return 'Required';
        if (typeof value === 'string' && !/^\+?[\d\s\-\(\)]{10,15}$/.test(value)) return 'Invalid phone format';
        break;
      case 'dateOfBirth':
        if (!value) return 'Required';
        if (typeof value === 'string') {
          const birthDate = new Date(value);
          const today = new Date();
          const age = today.getFullYear() - birthDate.getFullYear();
          if (age < 13) return 'Must be at least 13 years old';
          if (birthDate > today) return 'Date cannot be in the future';
        }
        break;
      case 'gender':
        if (!value) return 'Required';
        break;
      case 'street':
        if (!value) return 'Required';
        if (typeof value === 'string' && value.length > 200) return 'Max 200 characters';
        break;
      case 'city':
        if (!value) return 'Required';
        if (typeof value === 'string' && value.length > 100) return 'Max 100 characters';
        break;
      case 'state':
        if (!value) return 'Required';
        if (typeof value === 'string' && value.length > 50) return 'Max 50 characters';
        break;
      case 'zip':
        if (!value) return 'Required';
        if (typeof value === 'string' && !/^\w{3,10}$/.test(value)) return 'Invalid postal code';
        break;
      case 'emergencyName':
        if (value && typeof value === 'string' && value.length > 100) return 'Max 100 characters';
        break;
      case 'emergencyRelationship':
        if (value && typeof value === 'string' && value.length > 50) return 'Max 50 characters';
        break;
      case 'emergencyPhone':
        if (value && typeof value === 'string' && !/^\+?[\d\s\-\(\)]{10,15}$/.test(value)) return 'Invalid phone format';
        break;
      case 'password':
        if (!value) return 'Required';
        if (typeof value === 'string' && value.length < 8) return 'Min 8 characters';
        if (typeof value === 'string' && !/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(value)) {
          return 'Must include uppercase, lowercase, and number';
        }
        break;
      case 'confirmPassword':
        if (!value) return 'Required';
        if (typeof value === 'string' && value !== form.password) return 'Passwords do not match';
        break;
      case 'agreeToTerms':
        if (!value) return 'You must agree to the terms and conditions';
        break;
      case 'marketingConsent':
        // This field is optional, so no validation needed
        break;
      case 'dataSharingConsent':
        // This field is optional, so no validation needed
        break;
      case 'insuranceProvider':
        if (typeof value === 'string' && value.length > 100) return 'Max 100 characters';
        break;
      case 'policyNumber':
        if (typeof value === 'string' && value.length > 50) return 'Max 50 characters';
        break;
      case 'groupNumber':
        if (typeof value === 'string' && value.length > 50) return 'Max 50 characters';
        break;
      case 'planName':
        if (typeof value === 'string' && value.length > 100) return 'Max 100 characters';
        break;
      case 'memberId':
        if (typeof value === 'string' && value.length > 50) return 'Max 50 characters';
        break;
      case 'medicalHistory':
        if (typeof value === 'string' && value.length > 500) return 'Max 500 characters';
        break;
      default:
        break;
    }
    return '';
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    const checked = type === 'checkbox' ? (e.target as HTMLInputElement).checked : undefined;
    
    setForm((prev) => ({ 
      ...prev, 
      [name]: type === 'checkbox' ? checked : value 
    }));
    setErrors((prev) => ({ 
      ...prev, 
      [name]: validate(name, type === 'checkbox' ? (checked ? 'true' : '') : value) 
    }));
  };

  const handleBlur = (e: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setErrors((prev) => ({ ...prev, [name]: validate(name, value) }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    console.log('🚀 Patient registration form submitted');
    
    // Validate all required fields
    const newErrors: typeof errors = {};
    let hasError = false;
    
    for (const key in form) {
      if (key !== 'emergencyName' && key !== 'emergencyRelationship' && key !== 'emergencyPhone' && 
          key !== 'insuranceProvider' && key !== 'policyNumber' && key !== 'groupNumber' && 
          key !== 'planName' && key !== 'memberId' && key !== 'medicalHistory') {
        const err = validate(key, form[key as keyof typeof form]);
        newErrors[key] = err;
        if (err) hasError = true;
      }
    }
    
    // Validate emergency contact if provided
    if (form.emergencyName || form.emergencyRelationship || form.emergencyPhone) {
      const emergencyNameError = validate('emergencyName', form.emergencyName);
      const emergencyRelationshipError = validate('emergencyRelationship', form.emergencyRelationship);
      const emergencyPhoneError = validate('emergencyPhone', form.emergencyPhone);
      
      newErrors.emergencyName = emergencyNameError;
      newErrors.emergencyRelationship = emergencyRelationshipError;
      newErrors.emergencyPhone = emergencyPhoneError;
      
      if (emergencyNameError || emergencyRelationshipError || emergencyPhoneError) hasError = true;
    }
    
    if (!form.agreeToTerms) {
      newErrors.agreeToTerms = 'You must agree to the terms and conditions';
      hasError = true;
    }
    
    setErrors(newErrors);
    if (hasError) {
      console.log('❌ Validation errors found:', newErrors);
      return;
    }
    
    console.log('✅ Validation passed, starting API call...');
    setLoading(true);
    setApiError(null);

    try {
      // Calculate age from date of birth
      const birthDate = new Date(form.dateOfBirth);
      const today = new Date();
      const age = today.getFullYear() - birthDate.getFullYear();
      
      // Prepare data for API
      const registrationData = {
        first_name: form.firstName.trim(),
        last_name: form.lastName.trim(),
        email: form.email.trim(),
        phone_number: form.phone.trim(),
        password: form.password,
        confirm_password: form.confirmPassword,
        date_of_birth: form.dateOfBirth,
        gender: form.gender.toLowerCase().replace(/\s+/g, '_'),
        address: {
          street: form.street.trim(),
          city: form.city.trim(),
          state: form.state.trim(),
          zip: form.zip.trim()
        },
        emergency_contact: {
          name: form.emergencyName.trim() || '',
          phone: form.emergencyPhone.trim() || '',
          relationship: form.emergencyRelationship.trim() || '',
          complete: !!(form.emergencyName && form.emergencyPhone && form.emergencyRelationship)
        },
        medical_history: form.medicalHistory ? [form.medicalHistory.trim()] : [],
        insurance_info: {
          provider: form.insuranceProvider.trim() || '',
          policy_number: form.policyNumber.trim() || '',
          group_number: form.groupNumber.trim() || '',
          plan_name: form.planName.trim() || '',
          member_id: form.memberId.trim() || '',
          complete: !!(form.insuranceProvider && form.policyNumber && form.groupNumber)
        },
        marketing_consent: form.marketingConsent,
        data_sharing_consent: form.dataSharingConsent,
        valid_for_registration: true,
        valid_gender: true,
        gender_for_entity: form.gender,
        normalized_email: form.email.trim().toLowerCase(),
        age: age,
        normalized_phone_number: form.phone.trim(),
        sanitized_medical_history: form.medicalHistory ? [form.medicalHistory.trim()] : []
      };

      console.log('📝 Submitting patient registration data:', registrationData);
      const response = await patientAPI.register(registrationData);
      console.log('✅ Patient registration successful:', response.data);

      setLoading(false); // Add this line to reset loading state
      setApiSuccess('Registration successful! Welcome to MedCare. Redirecting to dashboard...');
      setIsSuccess(true);
      setRateLimitCountdown(null);

      setTimeout(() => {
        if (onRegistrationSuccess) {
          onRegistrationSuccess();
        }
        setForm(initialState);
        setErrors({});
        setApiSuccess(null);
        setIsSuccess(false);
      }, 2000);

    } catch (error: any) {
      console.error('❌ Patient registration failed:', error);
      setLoading(false);

      if (error.response) {
        const errorData = error.response.data;
        console.log('🔍 Error details:', errorData);

        if (errorData.errors) {
          // Map API validation errors (snake_case) to form errors (camelCase)
          const apiErrors: any = {};
          Object.keys(errorData.errors).forEach(key => {
            const formField = key === 'phone_number' ? 'phone' :
                            key === 'confirm_password' ? 'confirmPassword' :
                            key === 'first_name' ? 'firstName' :
                            key === 'last_name' ? 'lastName' :
                            key === 'date_of_birth' ? 'dateOfBirth' :
                            key === 'emergency_contact' ? 'emergencyName' :
                            key === 'insurance_info' ? 'insuranceProvider' :
                            key === 'medical_history' ? 'medicalHistory' :
                            key === 'marketing_consent' ? 'marketingConsent' :
                            key === 'data_sharing_consent' ? 'dataSharingConsent' : key;
            apiErrors[formField] = errorData.errors[key];
          });
          setErrors(apiErrors);
        } else {
          const errorMessage = errorData.message ||
                             errorData.error ||
                             `Registration failed (${error.response.status})`;

          if (errorData.error_code === 'RATE_LIMIT_EXCEEDED') {
            const retryAfter = errorData.retry_after_seconds || 0;
            const minutes = Math.ceil(retryAfter / 60);
            setApiError(`${errorMessage} Please try again in ${minutes} minutes.`);
            setRateLimitCountdown(retryAfter);
          } else {
            setApiError(errorMessage);
          }
        }
      } else if (error.request) {
        setApiError('Network error. Please check your internet connection and try again.');
      } else {
        setApiError('An unexpected error occurred. Please try again.');
      }
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
        <Typography variant="h5" fontWeight={700} color={theme.palette.secondary.main}>
          Patient Registration
        </Typography>
      </Box>
      <Box sx={{ px: 2, py: 2 }}>
        <form onSubmit={handleSubmit} autoComplete="off">
          {/* Personal Information */}
          <Typography variant="h6" fontWeight={600} mb={1} color="secondary">
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
                required
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
                required
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Email Address"
                name="email"
                type="email"
                value={form.email}
                onChange={handleChange}
                onBlur={handleBlur}
                error={!!errors.email}
                helperText={errors.email}
                fullWidth
                required
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
                required
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Date of Birth"
                name="dateOfBirth"
                type="date"
                value={form.dateOfBirth}
                onChange={handleChange}
                onBlur={handleBlur}
                error={!!errors.dateOfBirth}
                helperText={errors.dateOfBirth}
                fullWidth
                required
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                select
                label="Gender"
                name="gender"
                value={form.gender}
                onChange={handleChange}
                onBlur={handleBlur}
                error={!!errors.gender}
                helperText={errors.gender}
                fullWidth
                required
              >
                {genderOptions.map((option) => (
                  <MenuItem key={option} value={option}>{option}</MenuItem>
                ))}
              </TextField>
            </Grid>
          </Grid>
          <Divider sx={{ my: 2, bgcolor: 'grey.200' }} />
          
          {/* Address */}
          <Typography variant="h6" fontWeight={600} mb={1} color="secondary">
            Address
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
                required
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
                required
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
                required
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
                required
              />
            </Grid>
          </Grid>
          <Divider sx={{ my: 2, bgcolor: 'grey.200' }} />
          
          {/* Emergency Contact */}
          <Typography variant="h6" fontWeight={600} mb={1} color="secondary">
            Emergency Contact (Optional)
          </Typography>
          <Grid container spacing={2} mb={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Emergency Contact Name"
                name="emergencyName"
                value={form.emergencyName}
                onChange={handleChange}
                onBlur={handleBlur}
                error={!!errors.emergencyName}
                helperText={errors.emergencyName}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                select
                label="Relationship"
                name="emergencyRelationship"
                value={form.emergencyRelationship}
                onChange={handleChange}
                onBlur={handleBlur}
                error={!!errors.emergencyRelationship}
                helperText={errors.emergencyRelationship}
                fullWidth
              >
                {relationshipOptions.map((option) => (
                  <MenuItem key={option} value={option}>{option}</MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Emergency Phone Number"
                name="emergencyPhone"
                value={form.emergencyPhone}
                onChange={handleChange}
                onBlur={handleBlur}
                error={!!errors.emergencyPhone}
                helperText={errors.emergencyPhone}
                fullWidth
              />
            </Grid>
          </Grid>
          <Divider sx={{ my: 2, bgcolor: 'grey.200' }} />
          
          {/* Account Security */}
          <Typography variant="h6" fontWeight={600} mb={1} color="secondary">
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
                helperText={errors.password || "Min 8 characters, include uppercase, lowercase, and number"}
                fullWidth
                required
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
                required
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
          
          {/* Terms and Conditions */}
          <Box mb={3}>
            <FormControlLabel
              control={
                <Checkbox
                  name="agreeToTerms"
                  checked={form.agreeToTerms}
                  onChange={handleChange}
                  sx={{ color: theme.palette.secondary.main }}
                />
              }
              label={
                <Typography variant="body2">
                  I agree to the{' '}
                  <Link href="#" color="secondary" underline="hover">
                    Terms and Conditions
                  </Link>{' '}
                  and{' '}
                  <Link href="#" color="secondary" underline="hover">
                    Privacy Policy
                  </Link>
                </Typography>
              }
            />
            {errors.agreeToTerms && (
              <Typography variant="caption" color="error" sx={{ ml: 4, display: 'block' }}>
                {errors.agreeToTerms}
              </Typography>
            )}
          </Box>

          {/* Consent Options */}
          <Box mb={3}>
            <Typography variant="h6" fontWeight={600} mb={1} color="secondary">
              Consent & Preferences
            </Typography>
            <FormControlLabel
              control={
                <Checkbox
                  name="marketingConsent"
                  checked={form.marketingConsent}
                  onChange={handleChange}
                  sx={{ color: theme.palette.secondary.main }}
                />
              }
              label={
                <Typography variant="body2">
                  I consent to receive marketing communications about healthcare services and updates
                </Typography>
              }
            />
            <FormControlLabel
              control={
                <Checkbox
                  name="dataSharingConsent"
                  checked={form.dataSharingConsent}
                  onChange={handleChange}
                  sx={{ color: theme.palette.secondary.main }}
                />
              }
              label={
                <Typography variant="body2">
                  I consent to share my health data with authorized healthcare providers for treatment purposes
                </Typography>
              }
            />
          </Box>
          
          <Box textAlign="center" mt={2}>
            <Button
              type="submit"
              variant="contained"
              color="secondary"
              size="large"
              disabled={loading || (rateLimitCountdown !== null && rateLimitCountdown > 0)}
              sx={{ px: 6, py: 1.5, borderRadius: 2, fontWeight: 600 }}
              startIcon={loading ? <CircularProgress size={20} /> : null}
            >
              {loading ? 'Creating Account...' : 
               (rateLimitCountdown !== null && rateLimitCountdown > 0) ? 'Rate Limited' : 'Create Account'}
            </Button>
          </Box>
          
          {apiError && (
            <Alert severity="error" sx={{ mt: 2 }}>
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
            <Alert severity="success" sx={{ mt: 2 }}>
              {apiSuccess}
            </Alert>
          )}

          {isSuccess && (
            <Box sx={{
              position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
              bgcolor: 'rgba(76, 175, 80, 0.9)', display: 'flex', alignItems: 'center',
              justifyContent: 'center', zIndex: 9999, color: 'white'
            }}>
              <Box sx={{ textAlign: 'center' }}>
                <Typography variant="h4" fontWeight={700} sx={{ mb: 2 }}>🎉 Registration Successful!</Typography>
                <Typography variant="h6" sx={{ mb: 1 }}>Welcome to MedCare</Typography>
                <Typography variant="body1">Redirecting to dashboard...</Typography>
              </Box>
            </Box>
          )}

          <Box textAlign="center" mt={3}>
            <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>
              Already have an account?
            </Typography>
            <Button
              variant="outlined"
              color="secondary"
              size="medium"
              sx={{ borderRadius: 2, fontWeight: 500, textTransform: 'none' }}
              onClick={onBackToLogin}
            >
              Sign In
            </Button>
          </Box>
        </form>
      </Box>
    </FormPaper>
  );

  return (
    <RootContainer>
      <InfoSection>
        <Fade in timeout={900}>
          <Box>
            <Person sx={{ fontSize: 60, mb: 2, color: '#fff', filter: 'drop-shadow(0 2px 8px #4caf50)'}} />
            <Typography variant="h3" fontWeight={800} sx={{ letterSpacing: 1, mb: 1, color: '#fff' }}>
              Join MedCare
            </Typography>
            <Typography variant="h6" sx={{ color: '#e8f5e8', fontWeight: 400, mb: 2 }}>
              "Your Health Journey Starts Here"
            </Typography>
            <Typography variant="body1" sx={{ color: '#f1f8e9', maxWidth: 320, mx: 'auto', fontSize: 18 }}>
              Create your account to access personalized healthcare services, book appointments, and manage your medical records securely.
            </Typography>
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
          {loading ? (
            <Box display="flex" justifyContent="center" alignItems="center" height="100%">
              <CircularProgress sx={{ color: theme.palette.secondary.main }} />
            </Box>
          ) : (
            content
          )}
        </Fade>
      </Box>
    </RootContainer>
  );
};

export default PatientRegister; 