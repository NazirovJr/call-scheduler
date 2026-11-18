package com.callscheduler.company;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CompanyResponse create(@Valid @RequestBody CompanyRequest request) {
        return companyService.create(request);
    }

    @GetMapping
    Page<CompanyResponse> list(@RequestParam(required = false) CompanyStatus status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return companyService.list(status, page, size);
    }
}
