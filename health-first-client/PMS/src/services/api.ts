import axios from 'axios';

// API Base URL
const API_BASE_URL = 'https://d5a9e758940d.ngrok-free.app';

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'ngrok-skip-browser-warning': 'true', // Add this for ngrok
  },
  timeout: 10000, // 10 seconds timeout
});

// Request interceptor for logging
api.interceptors.request.use(
  (config) => {
    console.log('🚀 API Request:', {
      method: config.method?.toUpperCase(),
      url: config.url,
      data: config.data,
      headers: config.headers,
    });
    return config;
  },
  (error) => {
    console.error('❌ API Request Error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for logging
api.interceptors.response.use(
  (response) => {
    console.log('✅ API Response:', {
      status: response.status,
      url: response.config.url,
      data: response.data,
    });
    return response;
  },
  (error) => {
    console.error('❌ API Response Error:', {
      status: error.response?.status,
      url: error.config?.url,
      message: error.response?.data?.message || error.message,
      data: error.response?.data,
    });
    return Promise.reject(error);
  }
);

// Provider API endpoints
export const providerAPI = {
  // Health check
  health: () => api.get('/api/v1/provider/health'),
  
  // Test registration with minimal data
  testRegister: () => api.post('/api/v1/provider/register', {
    first_name: "Test",
    last_name: "User",
    email: "test@example.com",
    phone_number: "1234567890",
    password: "TestPass123!",
    confirm_password: "TestPass123!",
    specialization: "cardiology",
    license_number: "TEST123",
    years_of_experience: 5,
    clinic_address: {
      street: "123 Test St",
      city: "Test City",
      state: "Test State",
      zip: "12345"
    }
  }),
  
  // Registration
  register: (data: {
    first_name: string;
    last_name: string;
    email: string;
    phone_number: string;
    password: string;
    confirm_password: string;
    specialization: string;
    license_number: string;
    years_of_experience: number;
    clinic_address: {
      street: string;
      city: string;
      state: string;
      zip: string;
    };
  }) => api.post('/api/v1/provider/register', data),
  
  // Test login with minimal data
  testLogin: () => api.post('/api/v1/provider/login', {
    identifier: "test@example.com",
    password: "TestPass123!",
    rememberMe: true,
    deviceInfo: {
      deviceType: "web",
      deviceName: "Test Device",
      appVersion: "1.0.0",
      operatingSystem: "Windows",
      browser: "Chrome"
    },
    deviceName: "Test Device",
    deviceType: "web",
    appVersion: "1.0.0",
    operatingSystem: "Windows",
    emailIdentifier: true,
    phoneIdentifier: false,
    browser: "Chrome"
  }),
  
  // Login
  login: (data: { 
    identifier: string; 
    password: string; 
    rememberMe: boolean;
    deviceInfo: {
      deviceType: string;
      deviceName: string;
      appVersion: string;
      operatingSystem: string;
      browser: string;
    };
    deviceName: string;
    deviceType: string;
    appVersion: string;
    operatingSystem: string;
    emailIdentifier: boolean;
    phoneIdentifier: boolean;
    browser: string;
  }) => api.post('/api/v1/provider/login', data),
};

// Patient API endpoints
export const patientAPI = {
  // Health check
  health: () => api.get('/api/v1/patient/health'),
  
  // Test login with minimal data
  testLogin: () => api.post('/api/v1/patient/login', {
    identifier: "test@example.com",
    password: "TestPass123!",
    rememberMe: true,
    deviceInfo: {
      deviceType: "web",
      deviceName: "Test Device",
      appVersion: "1.0.0",
      operatingSystem: "Windows",
      browser: "Chrome"
    },
    identifierType: "email",
    emailIdentifier: true,
    phoneIdentifier: false,
    maskedIdentifier: "te***om",
    deviceFingerprint: "Test Device Windows",
    normalizedIdentifier: "test@example.com",
    validLoginRequest: true
  }),
  
  // Registration
  register: (data: {
    first_name: string;
    last_name: string;
    email: string;
    phone_number: string;
    password: string;
    confirm_password: string;
    date_of_birth: string;
    gender: string;
    address: {
      street: string;
      city: string;
      state: string;
      zip: string;
    };
    emergency_contact: {
      name: string;
      phone: string;
      relationship: string;
      complete: boolean;
    };
    medical_history: string[];
    insurance_info: {
      provider: string;
      policy_number: string;
      group_number: string;
      plan_name: string;
      member_id: string;
      complete: boolean;
    };
    marketing_consent: boolean;
    data_sharing_consent: boolean;
    valid_for_registration: boolean;
    valid_gender: boolean;
    gender_for_entity: string;
    normalized_email: string;
    age: number;
    normalized_phone_number: string;
    sanitized_medical_history: string[];
  }) => api.post('/api/v1/patient/register', data),
  
  // Login
  login: (data: {
    identifier: string;
    password: string;
    rememberMe: boolean;
    deviceInfo: {
      deviceType: string;
      deviceName: string;
      appVersion: string;
      operatingSystem: string;
      browser: string;
    };
    identifierType: string;
    emailIdentifier: boolean;
    phoneIdentifier: boolean;
    maskedIdentifier: string;
    deviceFingerprint: string;
    normalizedIdentifier: string;
    validLoginRequest: boolean;
  }) => api.post('/api/v1/patient/login', data),
};

export default api; 