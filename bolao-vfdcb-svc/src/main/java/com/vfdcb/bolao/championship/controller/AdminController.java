package com.vfdcb.bolao.championship.controller;

import com.vfdcb.bolao.championship.service.FootballDataSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final FootballDataSyncService syncService;

    public AdminController(FootballDataSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync-matches")
    public ResponseEntity<Void> syncMatches() {
        syncService.loadWorldCupData();
        return ResponseEntity.ok().build();
    }

}
