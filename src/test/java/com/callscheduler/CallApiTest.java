package com.callscheduler;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class CallApiTest extends ApiTest {

    private static final String TUE_10 = "2026-11-17T05:00:00Z";

    @Test
    void meetingSetCallRequiresCreatedMeeting() throws Exception {
        int company = createCompany();
        int foreignMeeting = id(bookMeeting(createCompany(), 1, TUE_10));

        call(company, "meeting_set", null).andExpect(status().isBadRequest());
        call(company, "meeting_set", 999).andExpect(status().isBadRequest());
        call(company, "meeting_set", foreignMeeting).andExpect(status().isBadRequest());

        int meeting = id(bookMeeting(company, 2, TUE_10));
        call(company, "meeting_set", meeting)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("meeting_set"))
                .andExpect(jsonPath("$.meetingId").value(meeting));
        companiesWithStatus("meeting_set").andExpect(jsonPath("$.content[*].id").value(contains(company)));
    }

    @Test
    void meetingSetCallRejectsCancelledMeeting() throws Exception {
        int company = createCompany();
        int meeting = id(bookMeeting(company, 1, TUE_10));
        cancel(meeting).andExpect(status().isOk());

        call(company, "meeting_set", meeting).andExpect(status().isBadRequest());
    }

    @Test
    void callResultBecomesCompanyStatus() throws Exception {
        int company = createCompany();

        call(company, "no_answer", null).andExpect(status().isCreated());
        companiesWithStatus("no_answer").andExpect(jsonPath("$.content[*].id").value(contains(company)));

        call(company, "refused", null).andExpect(status().isCreated());
        companiesWithStatus("refused").andExpect(jsonPath("$.content[*].id").value(contains(company)));
    }

    @Test
    void cancellingMeetingReturnsCompanyToNew() throws Exception {
        int company = createCompany();
        int meeting = id(bookMeeting(company, 1, TUE_10));
        call(company, "meeting_set", meeting).andExpect(status().isCreated());

        cancel(meeting).andExpect(status().isOk());

        companiesWithStatus("new").andExpect(jsonPath("$.content[*].id").value(contains(company)));
    }

    @Test
    void invalidCallsAreRejected() throws Exception {
        int company = createCompany();
        int meeting = id(bookMeeting(company, 1, TUE_10));

        call(company, "refused", meeting).andExpect(status().isBadRequest());
        call(999, "refused", null).andExpect(status().isNotFound());
        call(company, "busy", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("result")));
    }
}
