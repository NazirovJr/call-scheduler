package com.callscheduler.call;

import com.callscheduler.company.CompanyStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CallResult {
    NO_ANSWER(CompanyStatus.NO_ANSWER),
    REFUSED(CompanyStatus.REFUSED),
    MEETING_SET(CompanyStatus.MEETING_SET);

    private final CompanyStatus companyStatus;
}
