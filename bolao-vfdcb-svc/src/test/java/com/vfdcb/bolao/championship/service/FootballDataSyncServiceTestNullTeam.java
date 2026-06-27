package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.championship.client.FootballDataClient;
import com.vfdcb.bolao.championship.client.dto.*;
import com.vfdcb.bolao.championship.config.FootballDataProperties;
import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.repository.GuessRepository;
import com.vfdcb.bolao.championship.repository.MatchRepository;
import com.vfdcb.bolao.championship.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import java.time.ZonedDateTime;
import java.util.List;
import static org.mockito.Mockito.*;

public class FootballDataSyncServiceTestNullTeam {
    @Test
    public void testProcessMatchesNullTeams() {
        FootballDataClient client = mock(FootballDataClient.class);
        FootballDataProperties properties = mock(FootballDataProperties.class);
        TeamRepository teamRepository = mock(TeamRepository.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        MatchService matchService = mock(MatchService.class);

        FootballDataSyncService syncService = new FootballDataSyncService(client, properties, teamRepository, matchRepository, matchService);

        MatchDto matchDto = new MatchDto(100L, ZonedDateTime.now(), "TIMED", null, null, null, "FINAL", ZonedDateTime.now());
        CompetitionMatchesResponse response = new CompetitionMatchesResponse(List.of(matchDto));

        syncService.processMatches(response);
    }
}
