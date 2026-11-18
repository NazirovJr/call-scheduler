package com.callscheduler.company;

public record CompanyResponse(Long id, String name, String bin, String phone, CompanyStatus status) {

    static CompanyResponse from(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getBin(), company.getPhone(),
                company.getStatus());
    }
}
