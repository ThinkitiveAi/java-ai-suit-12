# HealthFirst - Complete Healthcare Management System

A comprehensive healthcare management system with both backend server and frontend client components built for secure provider and patient management.

## 🏗 Project Structure

```
java-ai-suit-12/
├── health-first-server/    # Spring Boot Backend API
│   ├── src/main/java/
│   ├── src/main/resources/
│   ├── pom.xml
│   └── Dockerfile
├── health-first-client/    # Frontend Client Application
│   └── [Frontend components]
├── README.md
└── API_TEST_DATA.md
```

## 🚀 Components

### 🔧 HealthFirst Server (Backend)
A robust Spring Boot backend API focusing on secure provider and patient registration with comprehensive validation and security features.

### 🎨 HealthFirst Client (Frontend)  
Modern frontend application for healthcare professionals and patients to interact with the system.

## 🚀 Features

### Backend Features (HealthFirst Server)
- ✅ **Secure Provider Registration** with email/phone/license uniqueness validation
- ✅ **Patient Registration & Authentication** with comprehensive validation
- ✅ **Provider Availability Management** with flexible scheduling
- ✅ **Comprehensive Input Validation** with sanitization and specialization verification
- ✅ **Password Security** with BCrypt hashing (12 salt rounds)
- ✅ **JWT Authentication** for secure API access
- ✅ **Email Verification** with secure tokens and HTML templates
- ✅ **Rate Limiting** (5 attempts per IP per hour for registration)
- ✅ **Audit Logging** for all registration attempts
- ✅ **Error Handling** with detailed validation messages
- ✅ **Security Features** including input sanitization and injection prevention

### Frontend Features (HealthFirst Client)
- Modern responsive user interface
- Provider and patient dashboards
- Appointment scheduling interface
- Real-time notifications
- Secure authentication flows

## 🛠 Tech Stack

### Backend (HealthFirst Server)
- **Framework**: Spring Boot 3.2.1
- **Database**: H2 (In-Memory for development), PostgreSQL (Production)
- **Security**: Spring Security with BCrypt
- **Authentication**: JWT tokens
- **Validation**: Jakarta Validation (Bean Validation)
- **Email**: Spring Mail with HTML templates
- **Rate Limiting**: Bucket4j
- **Phone Validation**: Google libphonenumber
- **Documentation**: OpenAPI/Swagger
- **Database Migration**: Liquibase

### Frontend (HealthFirst Client)
- **Framework**: [To be specified based on actual client]
- **State Management**: [To be specified]
- **UI Components**: [To be specified]
- **HTTP Client**: [To be specified]

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+ (for backend)
- Node.js and npm (for frontend, if applicable)
- SMTP server configuration (for email verification)

### 1. Clone the Repository
```bash
git clone <repository-url>
cd java-ai-suit-12
```

### 2. Setup Backend (HealthFirst Server)
```bash
cd health-first-server
mvn clean install
mvn spring-boot:run
```

The backend API will start on `http://localhost:8080`

### 3. Setup Frontend (HealthFirst Client)
```bash
cd health-first-client
# Follow client-specific setup instructions
```

### 4. Access Services
- **Backend API**: `http://localhost:8080`
- **H2 Console**: `http://localhost:8080/h2-console`
- **Frontend**: Check client documentation for port
- **API Documentation**: `http://localhost:8080/swagger-ui.html` (when available)

## 🐳 Docker Support

### Backend Docker Setup
```bash
cd health-first-server
docker build -t health-first-server .
docker run -p 8080:8080 health-first-server
```

### Full Stack with Docker Compose
```bash
# From project root
docker-compose up -d
```

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

### Backend Enhancements
- [x] JWT Authentication for provider login
- [x] Patient registration module
- [x] Provider availability management
- [ ] Appointment booking system
- [ ] Real-time notifications with WebSocket
- [ ] File upload for license documents
- [ ] Integration with external identity providers
- [ ] Advanced security features (2FA, device tracking)
- [ ] Payment processing integration
- [ ] Video consultation support
- [ ] Mobile app backend APIs

### Frontend Enhancements
- [ ] Responsive mobile-first design
- [ ] Progressive Web App (PWA) capabilities
- [ ] Real-time chat functionality
- [ ] Advanced calendar integrations
- [ ] Offline mode support
- [ ] Multi-language support
- [ ] Dark/light theme toggle
- [ ] Accessibility improvements (WCAG compliance)

### DevOps & Infrastructure
- [ ] Kubernetes deployment manifests
- [ ] CI/CD pipeline with GitHub Actions
- [ ] Monitoring and alerting (Prometheus/Grafana)
- [ ] Load balancing and auto-scaling
- [ ] Database replication and backup strategies
- [ ] Security scanning and compliance tools

## 🤝 Development Guidelines

### Backend Development (HealthFirst Server)
- Follow Spring Boot best practices
- Use validation annotations for input validation
- Implement comprehensive error handling
- Write meaningful audit logs
- Apply security-first approach
- Write unit and integration tests
- Document APIs with OpenAPI/Swagger

### Frontend Development (HealthFirst Client)
- Follow modern frontend development practices
- Implement responsive design principles
- Use component-based architecture
- Implement proper state management
- Write unit and end-to-end tests
- Follow accessibility guidelines

### Security Considerations (Full Stack)
- Never log sensitive information (passwords, tokens)
- Validate all inputs server-side and client-side
- Use parameterized queries to prevent SQL injection
- Implement rate limiting on all public endpoints
- Hash passwords with strong algorithms (BCrypt 12+ rounds)
- Use HTTPS in production
- Implement proper CORS policies
- Sanitize user inputs on both frontend and backend

### Development Workflow
1. **Feature Development**: Create feature branches from main
2. **Code Review**: All changes require peer review
3. **Testing**: Maintain test coverage above 80%
4. **Documentation**: Update README and API docs with changes
5. **Deployment**: Use Docker for consistent environments

## 📄 License

This project is part of the HealthFirst Healthcare Management System.

## 🆘 Support & Troubleshooting

### Backend Issues (HealthFirst Server)
1. **Check Application Logs**: Monitor console output for errors
2. **Database Issues**: Access H2 console at `http://localhost:8080/h2-console`
3. **API Testing**: Use provided curl commands or Postman collection
4. **Security Events**: Review audit logs for authentication failures
5. **Rate Limiting**: Check if requests are being throttled

### Frontend Issues (HealthFirst Client)
1. **Check Browser Console**: Look for JavaScript errors
2. **Network Issues**: Verify API connectivity in browser dev tools
3. **Authentication**: Ensure JWT tokens are properly stored and sent
4. **Responsive Issues**: Test on different screen sizes and devices

### Common Issues & Solutions
| Issue | Possible Cause | Solution |
|-------|---------------|----------|
| CORS Errors | Frontend/Backend domain mismatch | Check CORS configuration |
| Authentication Failures | Invalid JWT tokens | Check token expiration and refresh logic |
| Database Connection | H2 database issues | Restart application, check H2 console |
| Email Not Sending | SMTP configuration | Verify email credentials in application.properties |
| Rate Limiting | Too many requests | Wait for rate limit window to reset |

### Getting Help
- 📧 **Email Support**: Contact the development team
- 📚 **Documentation**: Check API documentation at `/swagger-ui.html`
- 🐛 **Bug Reports**: Create detailed issue reports with logs
- 💡 **Feature Requests**: Submit enhancement requests with use cases

### Monitoring & Health Checks
- **Backend Health**: `GET /api/v1/provider/health`
- **Spring Actuator**: `GET /actuator/health`
- **Database Status**: Access H2 console for real-time data
- **API Documentation**: Available at `/swagger-ui.html` (when configured)

---

**Built with ❤️ for healthcare professionals**

*Last Updated: January 2025* 