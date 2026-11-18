package com.callscheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.callscheduler.meeting.Meeting;
import com.callscheduler.meeting.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

// Время в запросах — UTC, Алматы = UTC+5: 05:00Z — это 10:00 по Алматы
class MeetingApiTest extends ApiTest {

    private static final String TUE_10 = "2026-11-17T05:00:00Z";
    private static final String WED_10 = "2026-11-18T05:00:00Z";
    private static final String LAST_FRI_10 = "2026-11-13T05:00:00Z";

    @Autowired
    private MeetingRepository meetings;

    @Test
    void managerCannotHaveOverlappingMeetings() throws Exception {
        bookMeeting(createCompany(), 1, TUE_10).andExpect(status().isCreated());

        bookMeeting(createCompany(), 1, "2026-11-17T05:30:00Z").andExpect(status().isConflict());
        bookMeeting(createCompany(), 1, "2026-11-17T04:30:00Z").andExpect(status().isConflict());
        // встык — не конфликт
        bookMeeting(createCompany(), 1, "2026-11-17T06:00:00Z").andExpect(status().isCreated());
        bookMeeting(createCompany(), 1, "2026-11-17T04:00:00Z").andExpect(status().isCreated());
        // у другого менеджера то же время свободно
        bookMeeting(createCompany(), 2, TUE_10).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @CsvSource({
            "2026-11-17T04:00:00Z, 201", // 09:00 — начало рабочего дня
            "2026-11-17T12:00:00Z, 201", // 17:00 — заканчивается ровно в 18:00
            "2026-11-17T03:59:00Z, 400", // 08:59
            "2026-11-17T12:01:00Z, 400", // 17:01
            "2026-11-17T12:30:00Z, 400", // 17:30
            "2026-11-21T05:00:00Z, 400", // суббота, 10:00
            "2026-11-20T19:30:00Z, 400", // пятница по UTC, но суббота 00:30 по Алматы
            "2026-11-22T23:30:00Z, 400", // воскресенье по UTC, но понедельник 04:30 по Алматы
    })
    void meetingMustFitIntoWorkingHours(String startsAt, int expectedStatus) throws Exception {
        bookMeeting(createCompany(), 1, startsAt).andExpect(status().is(expectedStatus));
    }

    @Test
    void refusalIsProblemDetailWithClearMessage() throws Exception {
        bookMeeting(createCompany(), 1, "2026-11-17T12:30:00Z")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("рабочие часы")));
    }

    @Test
    void meetingInThePastIsRejected() throws Exception {
        bookMeeting(createCompany(), 1, LAST_FRI_10).andExpect(status().isBadRequest());
    }

    @Test
    void companyCannotHaveTwoActiveMeetings() throws Exception {
        int company = createCompany();
        int meeting = id(bookMeeting(company, 1, TUE_10));

        bookMeeting(company, 2, WED_10).andExpect(status().isConflict());

        cancel(meeting).andExpect(status().isOk());
        bookMeeting(company, 2, WED_10).andExpect(status().isCreated());
    }

    @Test
    void pastMeetingIsNoLongerActive() throws Exception {
        int company = createCompany();
        // в обход API: через него встречу на прошедшее время не назначить
        Meeting past = meetings.save(new Meeting((long) company, 1L, Instant.parse(LAST_FRI_10),
                TestConfig.NOW.minus(Duration.ofDays(7))));

        bookMeeting(company, 2, TUE_10).andExpect(status().isCreated());
        cancel(past.getId().intValue()).andExpect(status().isConflict());
    }

    @Test
    void rescheduleKeepsHistory() throws Exception {
        int meeting = id(bookMeeting(createCompany(), 1, TUE_10));

        // сдвиг на полчаса пересекается только с собственным прежним временем
        reschedule(meeting, "2026-11-17T05:30:00Z").andExpect(status().isOk());
        reschedule(meeting, "2026-11-18T07:00:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsAt").value("2026-11-18T07:00:00Z"))
                .andExpect(jsonPath("$.endsAt").value("2026-11-18T08:00:00Z"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-16T00:00:00Z"))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.history[0].previousStartsAt").value(TUE_10))
                .andExpect(jsonPath("$.history[0].newStartsAt").value("2026-11-17T05:30:00Z"))
                .andExpect(jsonPath("$.history[1].previousStartsAt").value("2026-11-17T05:30:00Z"))
                .andExpect(jsonPath("$.history[1].newStartsAt").value("2026-11-18T07:00:00Z"));
    }

    @Test
    void rescheduleObeysSchedulingRules() throws Exception {
        bookMeeting(createCompany(), 1, WED_10).andExpect(status().isCreated());
        int meeting = id(bookMeeting(createCompany(), 1, TUE_10));

        reschedule(meeting, "2026-11-18T05:30:00Z").andExpect(status().isConflict());
        reschedule(meeting, "2026-11-17T12:30:00Z").andExpect(status().isBadRequest());
        reschedule(meeting, LAST_FRI_10).andExpect(status().isBadRequest());
        patch("/meetings/" + meeting, """
                {"action": "reschedule"}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void cancelledMeetingFreesSlotAndCannotBeChanged() throws Exception {
        int meeting = id(bookMeeting(createCompany(), 1, TUE_10));

        cancel(meeting)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));

        bookMeeting(createCompany(), 1, TUE_10).andExpect(status().isCreated());
        cancel(meeting).andExpect(status().isConflict());
        reschedule(meeting, WED_10).andExpect(status().isConflict());
    }

    @Test
    void scheduleListsScheduledMeetingsFirstThenCancelled() throws Exception {
        int cancelled = id(bookMeeting(createCompany(), 1, "2026-11-17T04:00:00Z"));
        cancel(cancelled).andExpect(status().isOk());
        int late = id(bookMeeting(createCompany(), 1, "2026-11-17T08:00:00Z"));
        int early = id(bookMeeting(createCompany(), 1, TUE_10));
        bookMeeting(createCompany(), 1, WED_10).andExpect(status().isCreated());
        bookMeeting(createCompany(), 2, TUE_10).andExpect(status().isCreated());

        mvc.perform(get("/managers/1/schedule").param("date", "2026-11-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(early, late, cancelled)))
                .andExpect(jsonPath("$[*].status").value(contains("scheduled", "scheduled", "cancelled")));
    }

    @Test
    void unknownEntitiesAndBadParametersAreReported() throws Exception {
        int company = createCompany();

        bookMeeting(999, 1, TUE_10).andExpect(status().isNotFound());
        bookMeeting(company, 999, TUE_10).andExpect(status().isNotFound());
        cancel(999).andExpect(status().isNotFound());
        mvc.perform(get("/managers/999/schedule").param("date", "2026-11-17")).andExpect(status().isNotFound());
        mvc.perform(get("/managers/1/schedule")).andExpect(status().isBadRequest());
        mvc.perform(get("/managers/1/schedule").param("date", "17.11.2026")).andExpect(status().isBadRequest());
        post("/meetings", """
                {"companyId": %d, "managerId": 1, "startsAt": "завтра"}
                """.formatted(company))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("startsAt")));
    }

    @Test
    void concurrentBookingsOfOneSlotCreateSingleMeeting() throws Exception {
        List<Callable<Integer>> requests = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int company = createCompany();
            requests.add(() -> bookMeeting(company, 1, TUE_10).andReturn().getResponse().getStatus());
        }

        assertThat(runInParallel(requests)).containsExactlyInAnyOrder(201, 409, 409, 409, 409, 409, 409, 409);
    }

    @Test
    void concurrentBookingsForOneCompanyCreateSingleMeeting() throws Exception {
        int company = createCompany();
        List<Callable<Integer>> requests = new ArrayList<>();
        for (int manager = 1; manager <= 3; manager++) {
            int managerId = manager;
            requests.add(() -> bookMeeting(company, managerId, TUE_10).andReturn().getResponse().getStatus());
        }

        assertThat(runInParallel(requests)).containsExactlyInAnyOrder(201, 409, 409);
    }

    private static List<Integer> runInParallel(List<Callable<Integer>> requests) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(requests.size())) {
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> response : executor.invokeAll(requests)) {
                statuses.add(response.get());
            }
            return statuses;
        }
    }
}
