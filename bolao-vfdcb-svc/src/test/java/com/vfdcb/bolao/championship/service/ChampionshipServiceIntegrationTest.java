package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.auth.model.User;
import com.vfdcb.bolao.auth.repository.UserRepository;
import com.vfdcb.bolao.championship.exception.MatchAlreadyStartedException;
import com.vfdcb.bolao.championship.model.*;
import com.vfdcb.bolao.championship.repository.GuessRepository;
import com.vfdcb.bolao.championship.repository.MatchRepository;
import com.vfdcb.bolao.championship.repository.TeamRepository;
import com.vfdcb.bolao.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ChampionshipServiceIntegrationTest {

    @Autowired
    private ChampionshipService championshipService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GuessRepository guessRepository;

    @Test
    void testIntegrationServiceListUpcomingMatches() {
        Team teamA = teamRepository.save(new Team("Team A", "TMA"));
        Team teamB = teamRepository.save(new Team("Team B", "TMB"));

        ZonedDateTime now = ZonedDateTime.now();
        Match match1 = matchRepository.save(new Match(teamA, teamB, now.plusHours(1), MatchStatus.TIMED));
        Match match2 = matchRepository.save(new Match(teamB, teamA, now.minusHours(1), MatchStatus.IN_PLAY));
        Match match3 = matchRepository.save(new Match(teamA, teamB, now.minusHours(2), MatchStatus.FINISHED));

        List<Match> matches = championshipService.listUpcomingMatches();

        boolean found1 = false, found2 = false, found3 = false;
        for (Match m : matches) {
            if (m.getId().equals(match1.getId())) found1 = true;
            if (m.getId().equals(match2.getId())) found2 = true;
            if (m.getId().equals(match3.getId())) found3 = true;
        }

        assertThat(found1).isTrue();
        assertThat(found2).isTrue();
        assertThat(found3).isTrue();
    }

    @Test
    void testIntegrationServiceGetUserGuesses() {
        User user = userRepository.save(new User("Test User", "test@user.com" + UUID.randomUUID(), "pass"));

        Team teamC = teamRepository.save(new Team("Team C", "TMC"));
        Team teamD = teamRepository.save(new Team("Team D", "TMD"));

        ZonedDateTime now = ZonedDateTime.now();
        Match match1 = matchRepository.save(new Match(teamC, teamD, now.plusHours(1), MatchStatus.TIMED));
        Match match2 = matchRepository.save(new Match(teamD, teamC, now.plusHours(2), MatchStatus.TIMED));

        championshipService.submitGuess(user.getId(), match1.getId(), 2, 1);

        List<GuessWithMatch> results = championshipService.getUserGuesses(user.getId());

        boolean found1 = false, found2 = false;
        for (GuessWithMatch r : results) {
            if (r.match().getId().equals(match1.getId())) {
                found1 = true;
                assertThat(r.guess().getHomeScore()).isEqualTo(2);
                assertThat(r.guess().getAwayScore()).isEqualTo(1);
                assertThat(r.guess().getId()).isNotNull();
            }
            if (r.match().getId().equals(match2.getId())) {
                found2 = true;
                assertThat(r.guess().getHomeScore()).isNull();
                assertThat(r.guess().getAwayScore()).isNull();
            }
        }

        assertThat(found1).isTrue();
        assertThat(found2).isTrue();
    }

    @Test
    void testIntegrationServiceSubmitGuess() {
        User user = userRepository.save(new User("Guess User", "guess@user.com" + UUID.randomUUID(), "pass"));

        Team teamE = teamRepository.save(new Team("Team E", "TME"));
        Team teamF = teamRepository.save(new Team("Team F", "TMF"));

        ZonedDateTime now = ZonedDateTime.now();
        Match matchFuture = matchRepository.save(new Match(teamE, teamF, now.plusHours(1), MatchStatus.TIMED));
        Match matchPast = matchRepository.save(new Match(teamE, teamF, now.minusSeconds(1), MatchStatus.TIMED));

        // Scenario A: Valid Guess
        Guess guess = championshipService.submitGuess(user.getId(), matchFuture.getId(), 1, 0);
        assertThat(guess).isNotNull();
        assertThat(guess.getHomeScore()).isEqualTo(1);

        // Scenario B: Update Guess
        Guess guessUpdated = championshipService.submitGuess(user.getId(), matchFuture.getId(), 3, 3);
        assertThat(guessUpdated.getId()).isEqualTo(guess.getId());
        assertThat(guessUpdated.getHomeScore()).isEqualTo(3);

        // Scenario C: Past Match
        assertThatThrownBy(() -> championshipService.submitGuess(user.getId(), matchPast.getId(), 1, 1))
                .isInstanceOf(MatchAlreadyStartedException.class);
    }

    @Test
    void testIntegrationServiceFinalizeMatch() {
        Team teamG = teamRepository.save(new Team("Team G", "TMG"));
        Team teamH = teamRepository.save(new Team("Team H", "TMH"));

        ZonedDateTime now = ZonedDateTime.now();
        Match match = matchRepository.save(new Match(teamG, teamH, now.plusHours(1), MatchStatus.TIMED));

        User userExact = userRepository.save(new User("User Exact", "ue@test.com" + UUID.randomUUID(), "pass"));
        User userWinnerDiff = userRepository.save(new User("User WinnerDiff", "uwd@test.com" + UUID.randomUUID(), "pass"));
        User userWinnerOnly = userRepository.save(new User("User WinnerOnly", "uwo@test.com" + UUID.randomUUID(), "pass"));
        User userWrong = userRepository.save(new User("User Wrong", "uw@test.com" + UUID.randomUUID(), "pass"));

        championshipService.submitGuess(userExact.getId(), match.getId(), 2, 1);
        championshipService.submitGuess(userWinnerDiff.getId(), match.getId(), 3, 2);
        championshipService.submitGuess(userWinnerOnly.getId(), match.getId(), 3, 0);
        championshipService.submitGuess(userWrong.getId(), match.getId(), 1, 1);

        // Finalize match with score 2-1
        Match updatedMatch = championshipService.finalizeMatch(match.getId(), 2, 1);
        assertThat(updatedMatch.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(updatedMatch.getHomeScore()).isEqualTo(2);

        // Verify points
        List<Guess> guesses = guessRepository.findByMatchId(match.getId());
        assertThat(guesses).hasSize(4);

        for (Guess g : guesses) {
            assertThat(g.getPoints()).isNotNull();
            if (g.getUserId().equals(userExact.getId())) assertThat(g.getPoints()).isEqualTo(4);
            if (g.getUserId().equals(userWinnerDiff.getId())) assertThat(g.getPoints()).isEqualTo(3);
            if (g.getUserId().equals(userWinnerOnly.getId())) assertThat(g.getPoints()).isEqualTo(2);
            if (g.getUserId().equals(userWrong.getId())) assertThat(g.getPoints()).isEqualTo(0);
        }
    }
}
