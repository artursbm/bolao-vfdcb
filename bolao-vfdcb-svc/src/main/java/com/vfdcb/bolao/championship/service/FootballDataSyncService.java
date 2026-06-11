package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.championship.client.FootballDataClient;
import com.vfdcb.bolao.championship.client.dto.CompetitionMatchesResponse;
import com.vfdcb.bolao.championship.client.dto.CompetitionTeamsResponse;
import com.vfdcb.bolao.championship.client.dto.MatchDto;
import com.vfdcb.bolao.championship.client.dto.TeamDto;
import com.vfdcb.bolao.championship.config.FootballDataProperties;
import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.MatchStatus;
import com.vfdcb.bolao.championship.model.Team;
import com.vfdcb.bolao.championship.repository.MatchRepository;
import com.vfdcb.bolao.championship.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FootballDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(FootballDataSyncService.class);

    private final FootballDataClient client;
    private final FootballDataProperties properties;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final MatchNotificationService notificationService;

    public FootballDataSyncService(FootballDataClient client,
                                   FootballDataProperties properties,
                                   TeamRepository teamRepository,
                                   MatchRepository matchRepository, 
                                   MatchService matchService,
                                   MatchNotificationService notificationService) {
        this.client = client;
        this.properties = properties;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.matchService = matchService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void loadWorldCupData() {
        CompetitionTeamsResponse teamsResponse = client.getCompetitionTeams(properties.getCompetitionId());
        processTeams(teamsResponse);

        CompetitionMatchesResponse response = client.getCompetitionMatches(properties.getCompetitionId());
        processMatches(response);
    }

    private void processTeams(CompetitionTeamsResponse teamsResponse) {
        if (teamsResponse != null && teamsResponse.teams() != null) {
            for (TeamDto teamDto : teamsResponse.teams()) {
                upsertTeam(teamDto);
            }
        }
    }

    public void processMatches(CompetitionMatchesResponse response) {
        if (response != null && response.matches() != null) {
            java.util.List<Team> allTeams = teamRepository.findAll();
            java.util.Map<Long, Team> teamMap = allTeams.stream()
                    .filter(t -> t.getExternalId() != null)
                    .collect(java.util.stream.Collectors.toMap(Team::getExternalId, t -> t, (t1, t2) -> t1));

            for (MatchDto matchDto : response.matches()
                    .stream().filter(m -> "GROUP_STAGE".equals(m.stage())).toList()) {
                Team homeTeam = matchDto.homeTeam() != null ? teamMap.get(matchDto.homeTeam().id()) : null;
                Team awayTeam = matchDto.awayTeam() != null ? teamMap.get(matchDto.awayTeam().id()) : null;
                upsertMatch(matchDto, homeTeam, awayTeam);
            }
        }
        boolean rankingsChanged = matchService.finalizeMatches();
        if (rankingsChanged) {
            notificationService.broadcastRankingUpdate();
        }
    }

    @Transactional
    @Scheduled(fixedRateString = "${football-data.sync-delay:120000}") // Default 5 minutes
    public void syncMatchesScheduled() {
        log.info("Starting Sync of matches");
        if (teamRepository.countTeams() == 0) {
            log.info("Teams not found, syncing teams first");
            CompetitionTeamsResponse teamsResponse = client.getCompetitionTeams(properties.getCompetitionId());
            processTeams(teamsResponse);
        }
        CompetitionMatchesResponse response = client.getCompetitionMatches(properties.getCompetitionId());
        processMatches(response);
        log.info("Finished Sync of matches");
    }

    private Team getTeam(TeamDto dto) {
        if (dto == null || dto.id() == null) return null;
        return teamRepository.findByExternalId(dto.id()).orElse(null);
    }

    private Team upsertTeam(TeamDto dto) {
        if (dto == null || dto.id() == null) return null;

        Optional<Team> existing = teamRepository.findByExternalId(dto.id());
        if (existing.isPresent()) {
            Team team = existing.get();
            team.setName(dto.name() != null ? dto.name() : team.getName());
            team.setCode(dto.tla() != null ? dto.tla() : team.getCode());
            team.setCrest(dto.crest() != null ? dto.crest() : team.getCrest());
            return teamRepository.save(team);
        } else {
            // Also check by code in case it was inserted manually
            if (dto.tla() != null) {
                Optional<Team> byCode = teamRepository.findByCode(dto.tla());
                if (byCode.isPresent()) {
                    Team team = byCode.get();
                    team.setExternalId(dto.id());
                    team.setName(dto.name() != null ? dto.name() : team.getName());
                    team.setCrest(dto.crest() != null ? dto.crest() : team.getCrest());
                    return teamRepository.save(team);
                }
            }
            Team newTeam = new Team();
            newTeam.setExternalId(dto.id());
            newTeam.setName(dto.name() != null ? dto.name() : "Unknown");
            newTeam.setCode(dto.tla() != null ? dto.tla() : "UNK");
            newTeam.setCrest(dto.crest());
            return teamRepository.save(newTeam);
        }
    }

    private void upsertMatch(MatchDto dto, Team homeTeam, Team awayTeam) {
        if (dto == null || dto.id() == null) return;

        Match match = matchRepository.findByExternalId(dto.id()).orElse(new Match());
        boolean isNew = match.getId() == null;
        Integer oldHomeScore = match.getHomeScore();
        Integer oldAwayScore = match.getAwayScore();
        MatchStatus oldStatus = match.getStatus();

        match.setExternalId(dto.id());
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);

        if (dto.utcDate() != null) {
            match.setMatchTime(dto.utcDate());
        }

        if (dto.status() != null) {
            try {
                match.setStatus(MatchStatus.valueOf(dto.status()));
            } catch (IllegalArgumentException e) {
                match.setStatus(MatchStatus.TIMED);
            }
        } else {
            match.setStatus(MatchStatus.TIMED);
        }

        if (dto.score() != null && dto.score().getFullTime() != null) {
            match.setHomeScore(dto.score().getFullTime().getHome());
            match.setAwayScore(dto.score().getFullTime().getAway());
        }

        Match savedMatch = matchRepository.save(match);

        boolean changed = isNew ||
                !java.util.Objects.equals(oldHomeScore, savedMatch.getHomeScore()) ||
                !java.util.Objects.equals(oldAwayScore, savedMatch.getAwayScore()) ||
                oldStatus != savedMatch.getStatus();

        if (changed) {
            notificationService.broadcastMatchUpdate(savedMatch);
        }
    }
}
