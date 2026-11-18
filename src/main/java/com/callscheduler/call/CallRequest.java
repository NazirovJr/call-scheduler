package com.callscheduler.call;

import jakarta.validation.constraints.NotNull;

public record CallRequest(@NotNull Long companyId, @NotNull CallResult result, Long meetingId) {
}
