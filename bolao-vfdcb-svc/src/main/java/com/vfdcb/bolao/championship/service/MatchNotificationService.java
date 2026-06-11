package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.championship.model.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MatchNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MatchNotificationService.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // Timeout 30 mins
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE Emitter completed");
            emitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE Emitter timed out");
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE Emitter error", e);
            emitters.remove(emitter);
        });

        // Send a connected event to ensure the client connects successfully
        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void broadcastMatchUpdate(Match match) {
        log.info("Broadcasting match update for match ID: {}", match.getId());
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("match-updated")
                        .data(match));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    public void broadcastRankingUpdate() {
        log.info("Broadcasting ranking update event");
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ranking-updated")
                        .data("updated"));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
