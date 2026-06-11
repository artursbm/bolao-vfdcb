package com.vfdcb.bolao.championship.controller;

import com.vfdcb.bolao.championship.service.MatchNotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
public class MatchStreamController {

    private final MatchNotificationService notificationService;

    public MatchStreamController(MatchNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping(path = "/matches", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMatches() {
        return notificationService.subscribe();
    }
}
