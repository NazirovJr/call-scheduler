package com.callscheduler.company;

import com.callscheduler.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CompanyRepository companies;

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        if (companies.existsByBin(request.bin())) {
            throw new ConflictException("Организация с БИН %s уже существует".formatted(request.bin()));
        }
        Company company = companies.save(new Company(request.name(), request.bin(), request.phone()));
        return CompanyResponse.from(company);
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> list(CompanyStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), Sort.by("id"));
        Page<Company> found = status == null
                ? companies.findAll(pageable)
                : companies.findByStatus(status, pageable);
        return found.map(CompanyResponse::from);
    }
}
