package com.callscheduler.call;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRepository extends JpaRepository<Call, Long> {

    boolean existsByMeetingId(Long meetingId);
}
