package com.healthfirst.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@healthfirst.com}")
    private String fromEmail;

    @Value("${app.email.verification-base-url:http://localhost:8080/api/v1/provider/verify}")
    private String verificationBaseUrl;

    @Value("${app.name:HealthFirst}")
    private String appName;

    /**
     * Send verification email to provider
     */
    public void sendProviderVerificationEmail(String toEmail, String firstName, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Provider Account - " + appName);

            String htmlContent = createVerificationEmailTemplate(firstName, verificationToken);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Send welcome email after verification
     */
    public void sendWelcomeEmail(String toEmail, String firstName, String lastName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to " + appName + " - Account Verified!");

            String htmlContent = createWelcomeEmailTemplate(firstName, lastName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome email as it's not critical
        }
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - " + appName);

            String htmlContent = createPasswordResetEmailTemplate(firstName, resetToken);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Send patient verification email
     */
    public void sendPatientVerificationEmail(String toEmail, String firstName, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Patient Account - " + appName);

            String htmlContent = createPatientVerificationEmailTemplate(firstName, verificationToken);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Patient verification email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send patient verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send patient verification email", e);
        }
    }

    private String createVerificationEmailTemplate(String firstName, String verificationToken) {
        String verificationUrl = verificationBaseUrl + "?token=" + verificationToken;
        String currentYear = String.valueOf(LocalDateTime.now().getYear());
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verify Your Account</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2c5aa0; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #2c5aa0; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 14px; color: #666; text-align: center; }
                    .warning { background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>%s</h1>
                    <h2>Account Verification Required</h2>
                </div>
                <div class="content">
                    <h3>Hello %s,</h3>
                    <p>Thank you for registering as a healthcare provider with %s. To complete your registration and start using our platform, please verify your email address.</p>
                    
                    <p><strong>Click the button below to verify your account:</strong></p>
                    <p style="text-align: center;">
                        <a href="%s" class="button">Verify My Account</a>
                    </p>
                    
                    <p>If the button doesn't work, you can copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; background-color: #f0f0f0; padding: 10px; border-radius: 3px;">%s</p>
                    
                    <div class="warning">
                        <strong>Important:</strong> This verification link will expire in 24 hours. If you didn't create this account, please ignore this email.
                    </div>
                    
                    <p>After verification, you'll be able to:</p>
                    <ul>
                        <li>Set up your availability schedule</li>
                        <li>Manage patient appointments</li>
                        <li>Access your provider dashboard</li>
                        <li>Update your profile information</li>
                    </ul>
                    
                    <p>If you have any questions or need assistance, please contact our support team.</p>
                    <p>Best regards,<br>The %s Team</p>
                </div>
                <div class="footer">
                    <p>© %s %s. All rights reserved.</p>
                    <p>This is an automated message, please do not reply to this email.</p>
                </div>
            </body>
            </html>
            """.formatted(appName, firstName, appName, verificationUrl, verificationUrl, appName, currentYear, appName);
    }

    private String createWelcomeEmailTemplate(String firstName, String lastName) {
        String currentYear = String.valueOf(LocalDateTime.now().getYear());
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to %s</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .footer { margin-top: 30px; font-size: 14px; color: #666; text-align: center; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Welcome to %s!</h1>
                    <h2>Account Successfully Verified</h2>
                </div>
                <div class="content">
                    <h3>Hello Dr. %s %s,</h3>
                    <p>Congratulations! Your provider account has been successfully verified and activated. You're now ready to start using our healthcare management platform.</p>
                    
                    <p><strong>What's next?</strong></p>
                    <ul>
                        <li>Log in to your dashboard and complete your profile</li>
                        <li>Set up your availability schedule</li>
                        <li>Review and configure your appointment settings</li>
                        <li>Start accepting patient appointments</li>
                    </ul>
                    
                    <p>We're excited to have you join our network of healthcare professionals committed to providing excellent patient care.</p>
                    
                    <p>If you need any assistance getting started, our support team is here to help.</p>
                    <p>Best regards,<br>The %s Team</p>
                </div>
                <div class="footer">
                    <p>© %s %s. All rights reserved.</p>
                </div>
            </body>
            </html>
            """.formatted(appName, appName, firstName, lastName, appName, currentYear, appName);
    }

    private String createPasswordResetEmailTemplate(String firstName, String resetToken) {
        String resetUrl = verificationBaseUrl.replace("/verify", "/reset-password") + "?token=" + resetToken;
        String currentYear = String.valueOf(LocalDateTime.now().getYear());
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset Request</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #dc3545; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #dc3545; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 14px; color: #666; text-align: center; }
                    .warning { background-color: #f8d7da; border: 1px solid #f5c6cb; padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Password Reset Request</h1>
                </div>
                <div class="content">
                    <h3>Hello %s,</h3>
                    <p>We received a request to reset your password for your %s provider account.</p>
                    
                    <p><strong>Click the button below to reset your password:</strong></p>
                    <p style="text-align: center;">
                        <a href="%s" class="button">Reset My Password</a>
                    </p>
                    
                    <div class="warning">
                        <strong>Security Notice:</strong> This reset link will expire in 1 hour. If you didn't request this password reset, please ignore this email and your password will remain unchanged.
                    </div>
                    
                    <p>Best regards,<br>The %s Team</p>
                </div>
                <div class="footer">
                    <p>© %s %s. All rights reserved.</p>
                </div>
            </body>
            </html>
            """.formatted(firstName, appName, resetUrl, appName, currentYear, appName);
    }

    private String createPatientVerificationEmailTemplate(String firstName, String verificationToken) {
        String verificationUrl = verificationBaseUrl.replace("/provider/verify", "/patient/verify") + "?token=" + verificationToken;
        String currentYear = String.valueOf(LocalDateTime.now().getYear());
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verify Your Patient Account</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #28a745; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 14px; color: #666; text-align: center; }
                    .warning { background-color: #d4edda; border: 1px solid #c3e6cb; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .privacy-note { background-color: #e7f3ff; border: 1px solid #b3d7ff; padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>%s</h1>
                    <h2>Patient Account Verification</h2>
                </div>
                <div class="content">
                    <h3>Hello %s,</h3>
                    <p>Welcome to %s! Thank you for creating your patient account. To complete your registration and access our healthcare services, please verify your email address.</p>
                    
                    <p><strong>Click the button below to verify your account:</strong></p>
                    <p style="text-align: center;">
                        <a href="%s" class="button">Verify My Patient Account</a>
                    </p>
                    
                    <p>If the button doesn't work, you can copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; background-color: #f0f0f0; padding: 10px; border-radius: 3px;">%s</p>
                    
                    <div class="warning">
                        <strong>Important:</strong> This verification link will expire in 24 hours. If you didn't create this account, please ignore this email.
                    </div>
                    
                    <div class="privacy-note">
                        <strong>Your Privacy Matters:</strong> We are committed to protecting your health information in compliance with HIPAA regulations. Your personal and medical information is secure and will never be shared without your consent.
                    </div>
                    
                    <p>Once verified, you'll be able to:</p>
                    <ul>
                        <li>Schedule appointments with healthcare providers</li>
                        <li>Access your medical records securely</li>
                        <li>Manage your health information</li>
                        <li>Receive important health updates</li>
                        <li>Connect with your healthcare team</li>
                    </ul>
                    
                    <p>If you have any questions or need assistance, please don't hesitate to contact our support team.</p>
                    
                    <p>Best regards,<br>The %s Team</p>
                </div>
                <div class="footer">
                    <p>© %s %s. All rights reserved.</p>
                    <p>Your health information is protected under HIPAA regulations.</p>
                </div>
            </body>
            </html>
            """.formatted(appName, firstName, appName, verificationUrl, verificationUrl, appName, currentYear, appName);
    }
} 