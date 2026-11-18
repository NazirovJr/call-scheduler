package com.callscheduler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
abstract class ApiTest {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JdbcTemplate jdbc;

    private int companyCount;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE calls, meeting_reschedules, meetings, companies RESTART IDENTITY");
    }

    protected ResultActions post(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions patch(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.patch(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected int createCompany() throws Exception {
        String bin = "%012d".formatted(++companyCount);
        return id(post("/companies", """
                {"name": "ТОО Ромашка", "bin": "%s", "phone": "+77011234567"}
                """.formatted(bin)).andExpect(status().isCreated()));
    }

    protected ResultActions companiesWithStatus(String status) throws Exception {
        return mvc.perform(get("/companies").param("status", status)).andExpect(status().isOk());
    }

    protected ResultActions bookMeeting(int companyId, int managerId, String startsAt) throws Exception {
        return post("/meetings", """
                {"companyId": %d, "managerId": %d, "startsAt": "%s"}
                """.formatted(companyId, managerId, startsAt));
    }

    protected ResultActions reschedule(int meetingId, String startsAt) throws Exception {
        return patch("/meetings/" + meetingId, """
                {"action": "reschedule", "startsAt": "%s"}
                """.formatted(startsAt));
    }

    protected ResultActions cancel(int meetingId) throws Exception {
        return patch("/meetings/" + meetingId, """
                {"action": "cancel"}
                """);
    }

    protected ResultActions call(int companyId, String result, Integer meetingId) throws Exception {
        return post("/calls", """
                {"companyId": %d, "result": "%s", "meetingId": %s}
                """.formatted(companyId, result, meetingId));
    }

    protected static int id(ResultActions created) throws Exception {
        return JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
