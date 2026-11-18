package com.callscheduler.manager;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Manager> findForUpdateById(Long id);
}
