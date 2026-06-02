package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.championship.client.FootballDataClient;
import com.vfdcb.bolao.championship.client.dto.CompetitionMatchesResponse;
import com.vfdcb.bolao.championship.client.dto.CompetitionTeamsResponse;
import com.vfdcb.bolao.championship.client.dto.MatchDto;
import com.vfdcb.bolao.championship.client.dto.ScoreDto;
import com.vfdcb.bolao.championship.client.dto.TeamDto;
import com.vfdcb.bolao.championship.config.FootballDataProperties;
import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.MatchStatus;
import com.vfdcb.bolao.championship.model.Team;
import com.vfdcb.bolao.championship.repository.MatchRepository;
import com.vfdcb.bolao.championship.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FootballDataSyncServiceTest {

    private FootballDataClient client;
    private FootballDataProperties properties;
    private TeamRepository teamRepository;
    private MatchRepository matchRepository;
    private FootballDataSyncService syncService;

    @BeforeEach
    void setUp() {
        client = mock(FootballDataClient.class);
        properties = mock(FootballDataProperties.class);
        teamRepository = mock(TeamRepository.class);
        matchRepository = mock(MatchRepository.class);
        syncService = new FootballDataSyncService(client, properties, teamRepository, matchRepository);
    }

    @Test
    void loadWorldCupData_shouldUpsertTeamsAndMatches() {
        when(properties.getCompetitionId()).thenReturn(2000L);

        CompetitionTeamsResponse teamsResponse = new CompetitionTeamsResponse();

        TeamDto homeTeamDto = new TeamDto();
        homeTeamDto.setId(10L);
        homeTeamDto.setName("Brazil");
        homeTeamDto.setTla("BRA");
        homeTeamDto.setCrest("http://bra.png");

        TeamDto awayTeamDto = new TeamDto();
        awayTeamDto.setId(20L);
        awayTeamDto.setName("Argentina");
        awayTeamDto.setTla("ARG");
        awayTeamDto.setCrest("http://arg.png");

        teamsResponse.setTeams(List.of(homeTeamDto, awayTeamDto));
        when(client.getCompetitionTeams(2000L)).thenReturn(teamsResponse);

        CompetitionMatchesResponse response = new CompetitionMatchesResponse();
        MatchDto matchDto = new MatchDto();
        matchDto.setId(100L);
        matchDto.setUtcDate(ZonedDateTime.now());
        matchDto.setStatus("FINISHED");

        TeamDto matchHomeTeamDto = new TeamDto();
        matchHomeTeamDto.setId(10L);
        matchHomeTeamDto.setName("Brazil");
        matchHomeTeamDto.setTla("BRA");
        matchDto.setHomeTeam(matchHomeTeamDto);

        TeamDto matchAwayTeamDto = new TeamDto();
        matchAwayTeamDto.setId(20L);
        matchAwayTeamDto.setName("Argentina");
        matchAwayTeamDto.setTla("ARG");
        matchDto.setAwayTeam(matchAwayTeamDto);

        ScoreDto scoreDto = new ScoreDto();
        ScoreDto.ScoreDetail scoreDetail = new ScoreDto.ScoreDetail();
        scoreDetail.setHome(2);
        scoreDetail.setAway(1);
        scoreDto.setFullTime(scoreDetail);
        matchDto.setScore(scoreDto);

        response.setMatches(List.of(matchDto));

        when(client.getCompetitionMatches(2000L)).thenReturn(response);
        
        Team t1 = new Team("Brazil", "BRA");
        t1.setExternalId(10L);
        when(teamRepository.findByExternalId(10L)).thenReturn(Optional.of(t1));
        
        Team t2 = new Team("Argentina", "ARG");
        t2.setExternalId(20L);
        when(teamRepository.findByExternalId(20L)).thenReturn(Optional.of(t2));
        
        when(teamRepository.findByCode(any())).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(i -> i.getArguments()[0]);

        when(matchRepository.findByExternalId(100L)).thenReturn(Optional.empty());

        syncService.loadWorldCupData();

        verify(client).getCompetitionTeams(2000L);
        verify(client).getCompetitionMatches(2000L);
        
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository, times(2)).save(teamCaptor.capture());
        
        List<Team> savedTeams = teamCaptor.getAllValues();
        assertEquals(10L, savedTeams.get(0).getExternalId());
        assertEquals("Brazil", savedTeams.get(0).getName());
        assertEquals(20L, savedTeams.get(1).getExternalId());
        assertEquals("Argentina", savedTeams.get(1).getName());

        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(matchCaptor.capture());

        Match savedMatch = matchCaptor.getValue();
        assertEquals(100L, savedMatch.getExternalId());
        assertEquals(MatchStatus.FINISHED, savedMatch.getStatus());
        assertEquals(2, savedMatch.getHomeScore());
        assertEquals(1, savedMatch.getAwayScore());
    }
}
