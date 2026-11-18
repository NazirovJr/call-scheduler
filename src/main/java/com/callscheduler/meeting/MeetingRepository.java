package com.callscheduler.meeting;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Meeting> findForUpdateById(Long id);

    @Query("""
            select count(m) > 0 from Meeting m
            where m.companyId = :companyId and m.status = SCHEDULED and m.endsAt > :now
            """)
    boolean existsActiveByCompanyId(Long companyId, Instant now);

    @Query("""
            select m from Meeting m
            where m.managerId = :managerId and m.status = SCHEDULED
              and m.startsAt < :endsAt and m.endsAt > :startsAt
            """)
    List<Meeting> findOverlapping(Long managerId, Instant startsAt, Instant endsAt);

    @EntityGraph(attributePaths = "history")
    @Query("""
            select m from Meeting m
            where m.managerId = :managerId and m.startsAt >= :dayStart and m.startsAt < :dayEnd
            order by case when m.status = SCHEDULED then 0 else 1 end, m.startsAt
            """)
    List<Meeting> findSchedule(Long managerId, Instant dayStart, Instant dayEnd);
}
