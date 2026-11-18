package com.callscheduler.company;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// ?status=new: в query-параметре статус приходит в нижнем регистре, как и в JSON
@Component
class CompanyStatusConverter implements Converter<String, CompanyStatus> {

    @Override
    public CompanyStatus convert(String source) {
        return CompanyStatus.valueOf(source.toUpperCase());
    }
}
