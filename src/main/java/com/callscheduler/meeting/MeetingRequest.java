package com.callscheduler.meeting;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record MeetingRequest(@NotNull Long companyId, @NotNull Long managerId, @NotNull Instant startsAt) {
}
