package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.championship.model.*;
import com.vfdcb.bolao.championship.repository.GuessRepository;
import com.vfdcb.bolao.championship.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private GuessRepository guessRepository;

    private MatchService matchService;

    @BeforeEach
    void setup() {
        ScoringConfig config = new ScoringConfig();
        config.setExact(4);
        config.setWinnerDiff(3);
        config.setWinner(2);
        config.setDraw(1);

        matchService = new MatchService(matchRepository, guessRepository, config);
    }

    @Test
    void finalizeMatches_CalculatesPoints() {
        UUID matchId = UUID.randomUUID();
        Match match = new Match(new Team(), new Team(), ZonedDateTime.now().minusHours(2), MatchStatus.FINISHED);
        match.setId(matchId);
        match.setHomeScore(2);
        match.setAwayScore(1);


        Guess guessExact = new Guess(UUID.randomUUID(), matchId, 2, 1); // Exact (4 pts)
        Guess guessWinnerDiff = new Guess(UUID.randomUUID(), matchId, 3, 2); // Winner + Diff (3 pts)
        Guess guessWinner = new Guess(UUID.randomUUID(), matchId, 3, 0); // Winner only, diff=3 vs real diff=1 (2 pts)
        Guess guessWrong = new Guess(UUID.randomUUID(), matchId, 0, 1); // Wrong (0 pts)

        when(guessRepository.findByMatchId(matchId)).thenReturn(List.of(guessExact, guessWinnerDiff, guessWinner, guessWrong));
        when(matchRepository.findAllByStatus(MatchStatus.FINISHED)).thenReturn(List.of(match));

        matchService.finalizeMatches();

        assertEquals(MatchStatus.FINISHED, match.getStatus());
        assertEquals(2, match.getHomeScore());
        assertEquals(1, match.getAwayScore());

        assertEquals(4, guessExact.getPoints());
        assertEquals(3, guessWinnerDiff.getPoints());
        assertEquals(2, guessWinner.getPoints());
        assertEquals(0, guessWrong.getPoints());

        verify(guessRepository, times(4)).save(any(Guess.class));
    }

}
