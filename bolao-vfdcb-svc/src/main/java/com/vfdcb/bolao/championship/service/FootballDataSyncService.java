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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class FootballDataSyncService {

    private final FootballDataClient client;
    private final FootballDataProperties properties;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    public FootballDataSyncService(FootballDataClient client,
                                   FootballDataProperties properties,
                                   TeamRepository teamRepository,
                                   MatchRepository matchRepository) {
        this.client = client;
        this.properties = properties;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public void loadWorldCupData() {
        CompetitionTeamsResponse teamsResponse = client.getCompetitionTeams(properties.getCompetitionId());
        if (teamsResponse != null && teamsResponse.teams() != null) {
            for (TeamDto teamDto : teamsResponse.teams()) {
                upsertTeam(teamDto);
            }
        }

        CompetitionMatchesResponse response = client.getCompetitionMatches(properties.getCompetitionId());
        processMatches(response);
    }

    public void processMatches(CompetitionMatchesResponse response) {
        if (response != null && response.matches() != null) {
            for (MatchDto matchDto : response.matches()
                    .stream().filter(m -> "GROUP_STAGE".equals(m.stage())).toList()) {
                Team homeTeam = getTeam(matchDto.homeTeam());
                Team awayTeam = getTeam(matchDto.awayTeam());
                upsertMatch(matchDto, homeTeam, awayTeam);
            }
        }
    }

    @Transactional
    @Scheduled(fixedDelayString = "${football-data.sync-delay:300000}") // Default 5 minutes
    public void syncMatchesScheduled() {
        CompetitionMatchesResponse response = client.getCompetitionMatches(properties.getCompetitionId());
        processMatches(response);
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

        matchRepository.save(match);
    }
}
