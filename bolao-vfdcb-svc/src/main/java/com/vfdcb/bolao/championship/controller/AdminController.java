package com.vfdcb.bolao.championship.controller;

import com.vfdcb.bolao.championship.dto.FinalizeMatchRequest;
import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.service.ChampionshipService;
import com.vfdcb.bolao.championship.service.FootballDataSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final FootballDataSyncService syncService;
    private final ChampionshipService championshipService;

    public AdminController(FootballDataSyncService syncService, ChampionshipService championshipService) {
        this.syncService = syncService;
        this.championshipService = championshipService;
    }

    @PostMapping("/sync-matches")
    public ResponseEntity<Void> syncMatches() {
        syncService.loadWorldCupData();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/match-results")
    public Match finalizeMatch(@Valid @RequestBody FinalizeMatchRequest req, HttpServletRequest request) {
        return championshipService.finalizeMatch(req.matchId(), req.homeScore(), req.awayScore());
    }
}
