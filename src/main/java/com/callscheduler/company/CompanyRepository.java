package com.callscheduler.company;

import java.util.Optional;

import com.callscheduler.error.NotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByBin(String bin);

    Page<Company> findByStatus(CompanyStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Company> findForUpdateById(Long id);

    default Company lockById(Long id) {
        return findForUpdateById(id)
                .orElseThrow(() -> new NotFoundException("Организация %d не найдена".formatted(id)));
    }
}
