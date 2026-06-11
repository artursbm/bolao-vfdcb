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

}
