package com.callscheduler.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CompanyRequest(
        @NotBlank String name,
        @NotNull @Pattern(regexp = "\\d{12}", message = "БИН должен состоять из 12 цифр") String bin,
        @NotNull @Pattern(regexp = "\\+?\\d{10,15}", message = "телефон: 10–15 цифр, допускается ведущий +") String phone) {
}
