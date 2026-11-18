package com.callscheduler.meeting;

import java.time.Instant;

import jakarta.persistence.Embeddable;

@Embeddable
public record Reschedule(Instant previousStartsAt, Instant newStartsAt, Instant rescheduledAt) {
}
