package rw.ticketing.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ticketing.dto.request.*;
import rw.ticketing.dto.response.AuthResponse;
import rw.ticketing.dto.response.MessageResponse;
import rw.ticketing.entity.*;
import rw.ticketing.enums.Role;
import rw.ticketing.exception.AppException;
import rw.ticketing.repository.*;
import rw.ticketing.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizerProfileRepository organizerProfileRepository;
    private final BuyerProfileRepository buyerProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw AppException.conflict("Email already registered");
        }
        if (req.getPhone() != null && userRepository.existsByPhone(req.getPhone())) {
            throw AppException.conflict("Phone number already in use");
        }
        if (req.getRole() == Role.ORGANIZER && (req.getBusinessName() == null || req.getBusinessName().isBlank())) {
            throw AppException.badRequest("Business name is required for organizer accounts");
        }

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail().toLowerCase())
                .phone(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .emailVerificationToken(UUID.randomUUID().toString())
                .build();
        userRepository.save(user);

        // Create the appropriate profile
        if (req.getRole() == Role.ORGANIZER) {
            OrganizerProfile profile = OrganizerProfile.builder()
                    .user(user)
                    .businessName(req.getBusinessName())
                    .build();
            organizerProfileRepository.save(profile);
        } else {
            BuyerProfile profile = BuyerProfile.builder()
                    .user(user)
                    .build();
            buyerProfileRepository.save(profile);
        }

        // Send verification email — failure is non-fatal with placeholder creds in dev
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), user.getEmailVerificationToken());
        } catch (Exception ignored) {}

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail().toLowerCase(), req.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw AppException.badRequest("Invalid email or password");
        }

        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (!user.isActive()) {
            throw AppException.forbidden("Account is disabled");
        }

        // Invalidate old refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        RefreshToken stored = refreshTokenRepository.findByToken(req.getRefreshToken())
                .orElseThrow(() -> AppException.badRequest("Invalid refresh token"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw AppException.badRequest("Refresh token expired, please log in again");
        }

        User user = stored.getUser();
        refreshTokenRepository.delete(stored);

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> AppException.badRequest("Invalid or expired verification token"));
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
        return new MessageResponse("Email verified successfully");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.getEmail().toLowerCase()).ifPresent(user -> {
            user.setPasswordResetToken(UUID.randomUUID().toString());
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(2));
            userRepository.save(user);
            try { emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), user.getPasswordResetToken()); } catch (Exception ignored) {}
        });
        // Always return success to prevent email enumeration
        return new MessageResponse("If that email exists, a reset link has been sent");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        User user = userRepository.findByPasswordResetToken(req.getToken())
                .orElseThrow(() -> AppException.badRequest("Invalid or expired reset token"));

        if (user.getPasswordResetExpiresAt() == null || LocalDateTime.now().isAfter(user.getPasswordResetExpiresAt())) {
            throw AppException.badRequest("Reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);

        // Invalidate all refresh tokens
        refreshTokenRepository.deleteByUser(user);

        return new MessageResponse("Password reset successfully");
    }

    private String createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        boolean hasOrganizer = organizerProfileRepository.existsByUserId(user.getId());
        boolean hasBuyer = buyerProfileRepository.existsByUserId(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .hasOrganizerProfile(hasOrganizer)
                .hasBuyerProfile(hasBuyer)
                .build();
    }
}
