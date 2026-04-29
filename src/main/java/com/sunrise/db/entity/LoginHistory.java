package com.sunrise.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "login_history")
public class LoginHistory {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name= "ip_address", nullable = false)
    @Size(max = 45)
    private String ipAddress;

    @Column(name= "device_info", nullable = false)
    private String deviceInfo;

    @Column(name= "login_at", nullable = false)
    private Instant loginAt = Instant.now();
}
