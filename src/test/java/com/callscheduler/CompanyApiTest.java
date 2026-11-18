package com.callscheduler;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class CompanyApiTest extends ApiTest {

    @Test
    void createsCompanyWithStatusNew() throws Exception {
        post("/companies", """
                {"name": "ТОО Ромашка", "bin": "123456789012", "phone": "+77011234567"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("ТОО Ромашка"))
                .andExpect(jsonPath("$.bin").value("123456789012"))
                .andExpect(jsonPath("$.phone").value("+77011234567"))
                .andExpect(jsonPath("$.status").value("new"));
    }

    @Test
    void duplicateBinIsRejected() throws Exception {
        String company = """
                {"name": "ТОО Ромашка", "bin": "123456789012", "phone": "+77011234567"}
                """;
        post("/companies", company).andExpect(status().isCreated());

        post("/companies", company).andExpect(status().isConflict());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"bin\": \"123456789012\", \"phone\": \"+77011234567\"}                        | name",
            "{\"name\": \"ТОО\", \"bin\": \"12345678901\", \"phone\": \"+77011234567\"}      | bin",
            "{\"name\": \"ТОО\", \"bin\": \"12345678901A\", \"phone\": \"+77011234567\"}     | bin",
            "{\"name\": \"ТОО\", \"bin\": \"123456789012\", \"phone\": \"позвоните мне\"}    | phone",
    })
    void invalidCompanyIsRejected(String json, String invalidField) throws Exception {
        post("/companies", json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(invalidField)));
    }

    @Test
    void listFiltersByStatusAndPaginates() throws Exception {
        int first = createCompany();
        int second = createCompany();
        int third = createCompany();
        call(second, "refused", null).andExpect(status().isCreated());

        companiesWithStatus("refused").andExpect(jsonPath("$.content[*].id").value(contains(second)));
        mvc.perform(get("/companies").param("status", "new").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains(third)))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2));
        mvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains(first, second, third)));
        mvc.perform(get("/companies").param("status", "unknown")).andExpect(status().isBadRequest());
    }

    @Test
    void standardRefusalsAreReportedInRussian() throws Exception {
        mvc.perform(get("/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ресурс не найден"));
        mvc.perform(delete("/companies"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.detail").value("Метод DELETE не поддерживается"));
        mvc.perform(MockMvcRequestBuilders.post("/companies").contentType(MediaType.TEXT_PLAIN).content("ТОО"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("Тип содержимого text/plain;charset=UTF-8 не поддерживается"));
    }
}
