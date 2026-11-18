package com.callscheduler.meeting;

import java.time.Instant;
import java.util.List;

public record MeetingResponse(Long id, Long companyId, Long managerId, Instant startsAt, Instant endsAt,
                              MeetingStatus status, Instant createdAt, List<Reschedule> history) {

    static MeetingResponse from(Meeting meeting) {
        return new MeetingResponse(meeting.getId(), meeting.getCompanyId(), meeting.getManagerId(),
                meeting.getStartsAt(), meeting.getEndsAt(), meeting.getStatus(), meeting.getCreatedAt(),
                List.copyOf(meeting.getHistory()));
    }
}
