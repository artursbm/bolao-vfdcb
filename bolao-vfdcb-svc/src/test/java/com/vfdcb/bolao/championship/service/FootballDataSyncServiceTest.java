package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.championship.client.FootballDataClient;
import com.vfdcb.bolao.championship.client.dto.*;
import com.vfdcb.bolao.championship.config.FootballDataProperties;
import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.MatchStatus;
import com.vfdcb.bolao.championship.model.ScoringConfig;
import com.vfdcb.bolao.championship.model.Team;
import com.vfdcb.bolao.championship.repository.GuessRepository;
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
        var config = new ScoringConfig();
        config.setExact(4);
        config.setWinnerDiff(3);
        config.setWinner(2);
        config.setDraw(1);
        MatchService matchService = new MatchService(matchRepository, mock(GuessRepository.class), config);

        syncService = new FootballDataSyncService(client, properties, teamRepository, matchRepository, matchService);
    }

    @Test
    void loadWorldCupData_shouldUpsertTeamsAndMatches() {
        when(properties.getCompetitionId()).thenReturn(2000L);

        TeamDto homeTeamDto = new TeamDto(10L,
                "Brazil",
                "BRA",
                "http://bra.png");

        TeamDto awayTeamDto = new TeamDto(20L,
                "Argentina",
                "ARG",
                "http://arg.png");

        CompetitionTeamsResponse teamsResponse = new CompetitionTeamsResponse(List.of(homeTeamDto, awayTeamDto));
        when(client.getCompetitionTeams(2000L)).thenReturn(teamsResponse);

        ScoreDto scoreDto = new ScoreDto();
        ScoreDto.ScoreDetail scoreDetail = new ScoreDto.ScoreDetail();
        scoreDetail.setHome(2);
        scoreDetail.setAway(1);
        scoreDto.setFullTime(scoreDetail);

        MatchDto matchDto = new MatchDto(100L,
                ZonedDateTime.now(),
                "FINISHED",
                homeTeamDto,
                awayTeamDto,
                scoreDto,
                "GROUP_STAGE",
                ZonedDateTime.now());

        CompetitionMatchesResponse response = new CompetitionMatchesResponse(List.of(matchDto));

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

    @Test
    void processMatches_shouldUpsertMatches() {
        TeamDto homeTeamDto = new TeamDto(10L, "Brazil", "BRA", "http://bra.png");
        TeamDto awayTeamDto = new TeamDto(20L, "Argentina", "ARG", "http://arg.png");

        ScoreDto scoreDto = new ScoreDto();
        ScoreDto.ScoreDetail scoreDetail = new ScoreDto.ScoreDetail();
        scoreDetail.setHome(1);
        scoreDetail.setAway(1);
        scoreDto.setFullTime(scoreDetail);

        MatchDto matchDto = new MatchDto(100L, ZonedDateTime.now(), "FINISHED", homeTeamDto, awayTeamDto, scoreDto, "GROUP_STAGE", ZonedDateTime.now());
        CompetitionMatchesResponse response = new CompetitionMatchesResponse(List.of(matchDto));

        Team t1 = new Team("Brazil", "BRA");
        t1.setExternalId(10L);
        when(teamRepository.findByExternalId(10L)).thenReturn(Optional.of(t1));

        Team t2 = new Team("Argentina", "ARG");
        t2.setExternalId(20L);
        when(teamRepository.findByExternalId(20L)).thenReturn(Optional.of(t2));

        when(matchRepository.findByExternalId(100L)).thenReturn(Optional.empty());

        syncService.processMatches(response);

        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(matchCaptor.capture());

        Match savedMatch = matchCaptor.getValue();
        assertEquals(100L, savedMatch.getExternalId());
        assertEquals(MatchStatus.FINISHED, savedMatch.getStatus());
        assertEquals(1, savedMatch.getHomeScore());
        assertEquals(1, savedMatch.getAwayScore());
    }
}
