package com.callscheduler.call;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
class CallController {

    private final CallService callService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CallResponse register(@Valid @RequestBody CallRequest request) {
        return callService.register(request);
    }
}
