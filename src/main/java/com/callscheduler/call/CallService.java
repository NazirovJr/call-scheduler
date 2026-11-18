package com.callscheduler.call;

import java.time.Clock;
import java.time.Instant;

import com.callscheduler.company.Company;
import com.callscheduler.company.CompanyRepository;
import com.callscheduler.error.InvalidRequestException;
import com.callscheduler.meeting.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository calls;
    private final CompanyRepository companies;
    private final MeetingRepository meetings;
    private final Clock clock;

    @Transactional
    public CallResponse register(CallRequest request) {
        Instant now = clock.instant();
        boolean meetingSet = request.result() == CallResult.MEETING_SET;
        if (meetingSet && request.meetingId() == null) {
            throw new InvalidRequestException("Звонок с результатом meeting_set требует meetingId созданной встречи");
        }
        if (!meetingSet && request.meetingId() != null) {
            throw new InvalidRequestException("meetingId допустим только для результата meeting_set");
        }
        Company company = companies.lockById(request.companyId());
        if (meetingSet) {
            meetings.findById(request.meetingId())
                    .filter(meeting -> meeting.getCompanyId().equals(company.getId()) && meeting.isActive(now))
                    .orElseThrow(() -> new InvalidRequestException(
                            "У организации %d нет активной встречи %d"
                                    .formatted(company.getId(), request.meetingId())));
        }
        company.setStatus(request.result().getCompanyStatus());
        Call call = calls.save(new Call(company.getId(), request.result(), request.meetingId(), now));
        return CallResponse.from(call);
    }
}
