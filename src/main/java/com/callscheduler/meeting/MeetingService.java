package com.callscheduler.meeting;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.callscheduler.company.CompanyRepository;
import com.callscheduler.error.ConflictException;
import com.callscheduler.error.InvalidRequestException;
import com.callscheduler.error.NotFoundException;
import com.callscheduler.manager.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Строки блокируются всегда в одном порядке: встреча → организация → менеджер, иначе возможен deadlock.
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetings;
    private final CompanyRepository companies;
    private final ManagerRepository managers;
    private final Clock clock;

    @Transactional
    public MeetingResponse create(MeetingRequest request) {
        Instant now = clock.instant();
        validateSlot(request.startsAt(), now);
        companies.lockById(request.companyId());
        lockManager(request.managerId());
        if (meetings.existsActiveByCompanyId(request.companyId(), now)) {
            throw new ConflictException(
                    "У организации %d уже есть активная встреча".formatted(request.companyId()));
        }
        requireManagerFree(request.managerId(), request.startsAt(), null);
        Meeting meeting = meetings.save(
                new Meeting(request.companyId(), request.managerId(), request.startsAt(), now));
        return MeetingResponse.from(meeting);
    }

    @Transactional
    public MeetingResponse update(Long id, MeetingPatchRequest request) {
        Instant now = clock.instant();
        Meeting meeting = switch (request.action()) {
            case RESCHEDULE -> reschedule(id, request.startsAt(), now);
            case CANCEL -> cancel(id, now);
        };
        return MeetingResponse.from(meeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> schedule(Long managerId, LocalDate date) {
        if (!managers.existsById(managerId)) {
            throw managerNotFound(managerId);
        }
        Instant dayStart = date.atStartOfDay(WorkingHours.ZONE).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(WorkingHours.ZONE).toInstant();
        return meetings.findSchedule(managerId, dayStart, dayEnd).stream()
                .map(MeetingResponse::from)
                .toList();
    }

    // Правило «одна активная встреча на организацию» здесь не проверяется:
    // переносится только активная встреча, а она у организации единственная.
    private Meeting reschedule(Long id, Instant startsAt, Instant now) {
        if (startsAt == null) {
            throw new InvalidRequestException("Для переноса встречи укажите startsAt");
        }
        validateSlot(startsAt, now);
        Meeting meeting = lockActiveMeeting(id, now);
        lockManager(meeting.getManagerId());
        requireManagerFree(meeting.getManagerId(), startsAt, id);
        meeting.reschedule(startsAt, now);
        return meeting;
    }

    private Meeting cancel(Long id, Instant now) {
        Meeting meeting = lockActiveMeeting(id, now);
        meeting.cancel();
        companies.lockById(meeting.getCompanyId()).meetingCancelled();
        return meeting;
    }

    private void validateSlot(Instant startsAt, Instant now) {
        if (!startsAt.isAfter(now)) {
            throw new InvalidRequestException("Время встречи должно быть в будущем");
        }
        if (!WorkingHours.fits(startsAt)) {
            throw new InvalidRequestException("Встреча длится 60 минут и должна целиком умещаться "
                    + "в рабочие часы: пн–пт, 09:00–18:00 по времени Asia/Almaty");
        }
    }

    private Meeting lockActiveMeeting(Long id, Instant now) {
        Meeting meeting = meetings.findForUpdateById(id)
                .orElseThrow(() -> new NotFoundException("Встреча %d не найдена".formatted(id)));
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new ConflictException("Встреча %d уже отменена".formatted(id));
        }
        if (!meeting.isActive(now)) {
            throw new ConflictException("Встреча %d уже прошла".formatted(id));
        }
        return meeting;
    }

    private void lockManager(Long id) {
        managers.findForUpdateById(id).orElseThrow(() -> managerNotFound(id));
    }

    private void requireManagerFree(Long managerId, Instant startsAt, Long ignoredMeetingId) {
        boolean busy = meetings.findOverlapping(managerId, startsAt, startsAt.plus(Meeting.DURATION)).stream()
                .anyMatch(meeting -> !meeting.getId().equals(ignoredMeetingId));
        if (busy) {
            throw new ConflictException(
                    "У менеджера %d уже есть встреча, пересекающаяся с этим временем".formatted(managerId));
        }
    }

    private static NotFoundException managerNotFound(Long id) {
        return new NotFoundException("Менеджер %d не найден".formatted(id));
    }
}
