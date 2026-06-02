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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

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
        if (teamsResponse != null && teamsResponse.getTeams() != null) {
            for (TeamDto teamDto : teamsResponse.getTeams()) {
                upsertTeam(teamDto);
            }
        }

        CompetitionMatchesResponse response = client.getCompetitionMatches(properties.getCompetitionId());
        if (response != null && response.getMatches() != null) {
            for (MatchDto matchDto : response.getMatches()
                    .stream().filter(m -> m.getStage().equals("GROUP_STAGE")).toList()) {
                Team homeTeam = getTeam(matchDto.getHomeTeam());
                Team awayTeam = getTeam(matchDto.getAwayTeam());
                upsertMatch(matchDto, homeTeam, awayTeam);
            }
        }
    }

    private Team getTeam(TeamDto dto) {
        if (dto == null || dto.getId() == null) return null;
        return teamRepository.findByExternalId(dto.getId()).orElse(null);
    }

    private Team upsertTeam(TeamDto dto) {
        if (dto == null || dto.getId() == null) return null;

        Optional<Team> existing = teamRepository.findByExternalId(dto.getId());
        if (existing.isPresent()) {
            Team team = existing.get();
            team.setName(dto.getName() != null ? dto.getName() : team.getName());
            team.setCode(dto.getTla() != null ? dto.getTla() : team.getCode());
            team.setCrest(dto.getCrest() != null ? dto.getCrest() : team.getCrest());
            return teamRepository.save(team);
        } else {
            // Also check by code in case it was inserted manually
            if (dto.getTla() != null) {
                Optional<Team> byCode = teamRepository.findByCode(dto.getTla());
                if (byCode.isPresent()) {
                    Team team = byCode.get();
                    team.setExternalId(dto.getId());
                    team.setName(dto.getName() != null ? dto.getName() : team.getName());
                    team.setCrest(dto.getCrest() != null ? dto.getCrest() : team.getCrest());
                    return teamRepository.save(team);
                }
            }
            Team newTeam = new Team();
            newTeam.setExternalId(dto.getId());
            newTeam.setName(dto.getName() != null ? dto.getName() : "Unknown");
            newTeam.setCode(dto.getTla() != null ? dto.getTla() : "UNK");
            newTeam.setCrest(dto.getCrest());
            return teamRepository.save(newTeam);
        }
    }

    private void upsertMatch(MatchDto dto, Team homeTeam, Team awayTeam) {
        if (dto == null || dto.getId() == null) return;

        Match match = matchRepository.findByExternalId(dto.getId()).orElse(new Match());
        match.setExternalId(dto.getId());
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);

        if (dto.getUtcDate() != null) {
            match.setMatchTime(LocalDateTime.ofInstant(dto.getUtcDate().toInstant(), ZoneId.systemDefault()));
        }

        if (dto.getStatus() != null) {
            try {
                match.setStatus(MatchStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                match.setStatus(MatchStatus.TIMED);
            }
        } else {
            match.setStatus(MatchStatus.TIMED);
        }

        if (dto.getScore() != null && dto.getScore().getFullTime() != null) {
            match.setHomeScore(dto.getScore().getFullTime().getHome());
            match.setAwayScore(dto.getScore().getFullTime().getAway());
        }

        matchRepository.save(match);
    }
}
