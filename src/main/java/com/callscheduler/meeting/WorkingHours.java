package com.callscheduler.meeting;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

final class WorkingHours {

    static final ZoneId ZONE = ZoneId.of("Asia/Almaty");
    private static final LocalTime OPEN = LocalTime.of(9, 0);
    private static final LocalTime CLOSE = LocalTime.of(18, 0);

    private WorkingHours() {
    }

    static boolean fits(Instant startsAt) {
        ZonedDateTime start = startsAt.atZone(ZONE);
        LocalTime time = start.toLocalTime();
        return start.getDayOfWeek().compareTo(DayOfWeek.FRIDAY) <= 0
                && !time.isBefore(OPEN)
                && !time.isAfter(CLOSE.minus(Meeting.DURATION));
    }
}
