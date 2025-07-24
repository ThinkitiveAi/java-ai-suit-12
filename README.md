# java-ai-suit-12

# HealthFirst Server - Provider Registration Module

A comprehensive healthcare management system backend built with Spring Boot, focusing on secure provider registration with comprehensive validation and security features.

## 🚀 Features

### Provider Registration Module
- ✅ **Secure Provider Registration** with email/phone/license uniqueness validation
- ✅ **Comprehensive Input Validation** with sanitization and specialization verification
- ✅ **Password Security** with BCrypt hashing (12 salt rounds)
- ✅ **Email Verification** with secure tokens and HTML templates
- ✅ **Rate Limiting** (5 attempts per IP per hour for registration)
- ✅ **Audit Logging** for all registration attempts
- ✅ **Error Handling** with detailed validation messages
- ✅ **Security Features** including input sanitization and injection prevention

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.2.1
- **Database**: H2 (In-Memory for development)
- **Security**: Spring Security with BCrypt
- **Validation**: Jakarta Validation (Bean Validation)
- **Email**: Spring Mail with HTML templates
- **Rate Limiting**: Bucket4j
- **Phone Validation**: Google libphonenumber
- **Documentation**: OpenAPI/Swagger (planned)

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+ (for building)
- SMTP server configuration (for email verification)

## 🚀 Quick Start

### 1. Clone and Setup
```bash
git clone <repository-url>
cd health-first-server
```

### 2. Install Dependencies
```bash
mvn clean install
```

### 3. Configure Email (Optional)
Update `src/main/resources/application.properties`:
```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
app.email.from=noreply@healthfirst.com
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 5. Access H2 Console (Development)
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:healthfirst`
- Username: `sa`
- Password: `password`

## 📡 API Endpoints

### Provider Registration

#### Register New Provider
```http
POST /api/v1/provider/register
Content-Type: application/json

{
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@clinic.com",
  "phone_number": "+1234567890",
  "password": "SecurePassword123!",
  "confirm_password": "SecurePassword123!",
  "specialization": "Cardiology",
  "license_number": "MD123456789",
  "years_of_experience": 10,
  "clinic_address": {
    "street": "123 Medical Center Dr",
    "city": "New York",
    "state": "NY",
    "zip": "10001"
  }
}
```

**Success Response (201):**
```json
{
  "success": true,
  "message": "Provider registered successfully. Verification email sent.",
  "data": {
    "provider_id": "uuid-here",
    "email": "john.doe@clinic.com",
    "verification_status": "pending"
  }
}
```

#### Verify Email
```http
GET /api/v1/provider/verify?token={verification_token}
```

#### Resend Verification Email
```http
POST /api/v1/provider/resend-verification?email={email}
```

#### Health Check
```http
GET /api/v1/provider/health
```

## 🔒 Security Features

### Input Validation
- Email format and uniqueness validation
- Phone number international format validation
- Password strength requirements (8+ chars, uppercase, lowercase, number, special char)
- License number alphanumeric validation
- Specialization from predefined list
- Input sanitization to prevent injection attacks

### Rate Limiting
- **Registration**: 5 attempts per IP per hour
- **Login**: 5 attempts per IP per 15 minutes
- Progressive blocking with increased duration for repeated violations

### Password Security
- BCrypt hashing with 12 salt rounds
- Secure password validation patterns
- Password confirmation verification

### Email Verification
- Secure token generation (32 characters)
- 24-hour token expiration
- HTML email templates with security warnings

## 🗄 Database Schema

### Provider Table
```sql
CREATE TABLE providers (
  id UUID PRIMARY KEY,
  first_name VARCHAR(50) NOT NULL,
  last_name VARCHAR(50) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  phone_number VARCHAR(20) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  specialization VARCHAR(100) NOT NULL,
  license_number VARCHAR(50) UNIQUE NOT NULL,
  years_of_experience INTEGER NOT NULL,
  clinic_street VARCHAR(200) NOT NULL,
  clinic_city VARCHAR(100) NOT NULL,
  clinic_state VARCHAR(50) NOT NULL,
  clinic_zip VARCHAR(10) NOT NULL,
  verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  license_document_url VARCHAR(500),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  verification_token VARCHAR(255),
  verification_token_expires_at TIMESTAMP,
  failed_login_attempts INTEGER NOT NULL DEFAULT 0,
  locked_until TIMESTAMP,
  last_login TIMESTAMP,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
```

## 🧪 Testing

### Test Provider Registration
```bash
curl -X POST http://localhost:8080/api/v1/provider/register \
  -H "Content-Type: application/json" \
  -d '{
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@test.com",
    "phone_number": "+1234567890",
    "password": "SecurePassword123!",
    "confirm_password": "SecurePassword123!",
    "specialization": "cardiology",
    "license_number": "MD123456789",
    "years_of_experience": 10,
    "clinic_address": {
      "street": "123 Medical Center Dr",
      "city": "New York",
      "state": "NY",
      "zip": "10001"
    }
  }'
```

### Test Rate Limiting
Make 6 consecutive registration requests to see rate limiting in action.

### Test Validation Errors
```bash
curl -X POST http://localhost:8080/api/v1/provider/register \
  -H "Content-Type: application/json" \
  -d '{
    "first_name": "J",
    "email": "invalid-email",
    "password": "weak"
  }'
```

## 🎯 Valid Specializations

The system validates against these medical specializations:
- cardiology, dermatology, endocrinology, gastroenterology, hematology
- infectious diseases, nephrology, neurology, oncology, pulmonology
- rheumatology, family medicine, internal medicine, pediatrics
- obstetrics and gynecology, psychiatry, surgery, orthopedics
- anesthesiology, radiology, pathology, emergency medicine
- urology, ophthalmology, otolaryngology, plastic surgery

## 📊 Monitoring & Logging

### Audit Logs
All registration attempts are logged with:
- Timestamp and IP address
- Success/failure status
- Masked email addresses for privacy
- Detailed error information

### Rate Limiting Logs
- Rate limit violations tracked per IP
- Progressive blocking implemented
- Suspicious activity detection

### Health Endpoints
- `/api/v1/provider/health` - Service health check
- `/actuator/health` - Spring Boot actuator health

## 🔧 Configuration

### Application Properties
Key configurations in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:h2:mem:healthfirst

# Email
spring.mail.host=smtp.gmail.com
app.email.from=noreply@healthfirst.com

# Rate Limiting
app.rate-limit.registration.attempts=5
app.rate-limit.registration.window-hours=1

# Security
app.security.password.min-length=8
app.security.account-lockout.max-attempts=5
```

## 🚧 Future Enhancements

- [ ] JWT Authentication for provider login
- [ ] Patient registration module
- [ ] Provider availability management
- [ ] Appointment booking system
- [ ] Real-time notifications
- [ ] File upload for license documents
- [ ] Integration with external identity providers
- [ ] Advanced security features (2FA, device tracking)

## 🤝 Development Guidelines

### Code Style
- Follow Spring Boot best practices
- Use validation annotations for input validation
- Implement comprehensive error handling
- Write meaningful audit logs
- Apply security-first approach

### Security Considerations
- Never log sensitive information (passwords, tokens)
- Validate all inputs server-side
- Use parameterized queries to prevent SQL injection
- Implement rate limiting on all public endpoints
- Hash passwords with strong algorithms (BCrypt 12+ rounds)

## 📄 License

This project is part of the HealthFirst Healthcare Management System.

## 🆘 Support

For support and questions:
1. Check the logs in the console output
2. Verify H2 console for database state
3. Test API endpoints with provided curl commands
4. Review audit logs for security events

---

**Built with ❤️ for healthcare professionals** 