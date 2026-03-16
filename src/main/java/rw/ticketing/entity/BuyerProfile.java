package rw.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buyer_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BuyerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "preferred_language", nullable = false, length = 10)
    @Builder.Default
    private String preferredLanguage = "en";

    @Column(name = "preferred_currency", nullable = false, length = 3)
    @Builder.Default
    private String preferredCurrency = "RWF";

    @Column(name = "default_payment_phone")
    private String defaultPaymentPhone;

    @Column(name = "default_payment_provider")
    private String defaultPaymentProvider;

    @Column(name = "receive_email_notifications", nullable = false)
    @Builder.Default
    private boolean receiveEmailNotifications = true;

    @Column(name = "receive_sms_notifications", nullable = false)
    @Builder.Default
    private boolean receiveSmsNotifications = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
