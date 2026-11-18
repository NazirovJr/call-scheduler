package com.callscheduler.call;

import java.time.Instant;

public record CallResponse(Long id, Long companyId, CallResult result, Long meetingId, Instant createdAt) {

    static CallResponse from(Call call) {
        return new CallResponse(call.getId(), call.getCompanyId(), call.getResult(), call.getMeetingId(),
                call.getCreatedAt());
    }
}
