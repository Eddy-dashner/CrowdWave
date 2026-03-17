package rw.ticketing.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h1 style="font-size: 24px; color: #1a1a1a;">Welcome to Tickwet 🎉</h1>
              <p style="font-size: 16px; color: #444;">Hi %s,</p>
              <p style="font-size: 16px; color: #444;">
                Thanks for signing up! Please confirm your email address to activate your account.
              </p>
              <div style="margin: 32px 0;">
                <a href="%s" style="display: inline-block; padding: 14px 28px; background: #6C47FF;
                   color: white; border-radius: 8px; text-decoration: none;
                   font-size: 16px; font-weight: bold;">
                  Confirm my account
                </a>
              </div>
              <p style="font-size: 14px; color: #888;">Or copy this link into your browser:</p>
              <p style="font-size: 13px; color: #6C47FF; word-break: break-all;">%s</p>
              <hr style="border: none; border-top: 1px solid #eee; margin: 32px 0;" />
              <p style="font-size: 12px; color: #aaa;">
                This link expires in 24 hours. If you did not create a Tickwet account, ignore this email.
              </p>
            </div>
            """.formatted(firstName, link, link);
        send(toEmail, "Confirm your Tickwet account", html);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px;">
                  <h2>Hello %s,</h2>
                  <p>You requested a password reset. Click below to set a new password.</p>
                  <a href="%s" style="display:inline-block; padding:12px 24px; background:#6C47FF;
                     color:white; border-radius:6px; text-decoration:none; font-weight:bold;">
                    Reset Password
                  </a>
                  <p style="color:#888; margin-top:24px; font-size:13px;">
                    This link expires in 2 hours. If you did not request this, ignore this email.
                  </p>
                </div>
                """.formatted(firstName, link);
        send(toEmail, "Reset your password — Rwanda Tickets", html);
    }

    @Async
    public void sendTicketConfirmation(String toEmail, String firstName, String eventTitle,
                                       String ticketCode, String qrCodeBase64) {
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px;">
                  <h2>Your ticket is confirmed! 🎉</h2>
                  <p>Hello %s, here is your ticket for <strong>%s</strong>.</p>
                  <div style="background:#f5f5f5; padding:16px; border-radius:8px; margin:16px 0;">
                    <p style="font-size:20px; font-weight:bold; letter-spacing:3px; text-align:center;">%s</p>
                  </div>
                  <img src="data:image/png;base64,%s" alt="QR Code" width="200" style="display:block; margin:0 auto;" />
                  <p style="color:#888; font-size:13px; text-align:center;">
                    Show this QR code at the entrance.
                  </p>
                </div>
                """.formatted(firstName, eventTitle, ticketCode, qrCodeBase64);
        send(toEmail, "Your ticket for " + eventTitle, html);
    }

    private void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
