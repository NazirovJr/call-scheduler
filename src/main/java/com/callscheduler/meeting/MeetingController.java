package com.callscheduler.meeting;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    MeetingResponse create(@Valid @RequestBody MeetingRequest request) {
        return meetingService.create(request);
    }

    @PatchMapping("/meetings/{id}")
    MeetingResponse update(@PathVariable Long id, @Valid @RequestBody MeetingPatchRequest request) {
        return meetingService.update(id, request);
    }

    @GetMapping("/managers/{id}/schedule")
    List<MeetingResponse> schedule(@PathVariable Long id,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return meetingService.schedule(id, date);
    }
}
