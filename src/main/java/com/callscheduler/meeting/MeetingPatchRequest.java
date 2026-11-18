package com.callscheduler.meeting;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record MeetingPatchRequest(@NotNull MeetingAction action, Instant startsAt) {
}
