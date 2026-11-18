package com.callscheduler.meeting;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meetings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting {

    public static final Duration DURATION = Duration.ofMinutes(60);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long companyId;
    private Long managerId;
    private Instant startsAt;
    private Instant endsAt;
    @Enumerated(EnumType.STRING)
    private MeetingStatus status = MeetingStatus.SCHEDULED;
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "meeting_reschedules", joinColumns = @JoinColumn(name = "meeting_id"))
    @OrderColumn(name = "seq")
    private List<Reschedule> history = new ArrayList<>();

    public Meeting(Long companyId, Long managerId, Instant startsAt, Instant createdAt) {
        this.companyId = companyId;
        this.managerId = managerId;
        this.createdAt = createdAt;
        moveTo(startsAt);
    }

    public boolean isActive(Instant now) {
        return status == MeetingStatus.SCHEDULED && endsAt.isAfter(now);
    }

    public void reschedule(Instant newStartsAt, Instant now) {
        history.add(new Reschedule(startsAt, newStartsAt, now));
        moveTo(newStartsAt);
    }

    public void cancel() {
        status = MeetingStatus.CANCELLED;
    }

    private void moveTo(Instant newStartsAt) {
        startsAt = newStartsAt;
        endsAt = newStartsAt.plus(DURATION);
    }
}
