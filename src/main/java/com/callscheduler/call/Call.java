package com.callscheduler.call;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "calls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long companyId;
    @Enumerated(EnumType.STRING)
    private CallResult result;
    private Long meetingId;
    private Instant createdAt;

    public Call(Long companyId, CallResult result, Long meetingId, Instant createdAt) {
        this.companyId = companyId;
        this.result = result;
        this.meetingId = meetingId;
        this.createdAt = createdAt;
    }
}
