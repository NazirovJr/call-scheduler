package com.callscheduler.company;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String bin;
    private String phone;
    @Setter
    @Enumerated(EnumType.STRING)
    private CompanyStatus status = CompanyStatus.NEW;

    public Company(String name, String bin, String phone) {
        this.name = name;
        this.bin = bin;
        this.phone = phone;
    }

    public void meetingCancelled() {
        if (status == CompanyStatus.MEETING_SET) {
            status = CompanyStatus.NEW;
        }
    }
}
