package rw.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizer_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "instagram_handle")
    private String instagramHandle;

    @Column(name = "twitter_handle")
    private String twitterHandle;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // Payout / settlement
    @Column(name = "payout_phone")
    private String payoutPhone;

    @Column(name = "payout_provider")
    private String payoutProvider;

    @Column(name = "payout_account_name")
    private String payoutAccountName;

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

    public void verify() {
        this.isVerified = true;
        this.verifiedAt = LocalDateTime.now();
    }
}
