package com.vfdcb.bolao.championship.repository;

import com.vfdcb.bolao.auth.model.User;
import com.vfdcb.bolao.auth.repository.UserRepository;
import com.vfdcb.bolao.championship.model.Guess;
import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.MatchStatus;
import com.vfdcb.bolao.championship.model.Team;
import com.vfdcb.bolao.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
public class GuessRepositoryIntegrationTest {

    @Autowired
    private GuessRepository guessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void testIntegrationRepositoryGetRanking() {
        // 1. Setup Data - Users
        User alice = userRepository.save(new User("Alice", "alice@test.com", "pass"));
        User bob = userRepository.save(new User("Bob", "bob@test.com", "pass"));
        User charlie = userRepository.save(new User("Charlie", "charlie@test.com", "pass"));

        // 2. Setup Data - Teams & Matches
        Team teamA = teamRepository.save(new Team("Test Team A", "TTA"));
        Team teamB = teamRepository.save(new Team("Test Team B", "TTB"));

        ZonedDateTime now = ZonedDateTime.now();
        Match match1 = matchRepository.save(new Match(teamA, teamB, now.plusHours(1), MatchStatus.TIMED));
        Match match2 = matchRepository.save(new Match(teamB, teamA, now.plusHours(2), MatchStatus.TIMED));

        // 3. Setup Data - Guesses (with computed points)
        Guess guessA1 = new Guess(alice.getId(), match1.getId(), 2, 1);
        guessA1.setPoints(4);
        guessRepository.save(guessA1);

        Guess guessA2 = new Guess(alice.getId(), match2.getId(), 1, 0);
        guessA2.setPoints(3);
        guessRepository.save(guessA2);

        Guess guessB1 = new Guess(bob.getId(), match1.getId(), 0, 3);
        guessB1.setPoints(0);
        guessRepository.save(guessB1);

        Guess guessB2 = new Guess(bob.getId(), match2.getId(), 2, 0);
        guessB2.setPoints(2);
        guessRepository.save(guessB2);

        // Charlie has NO guesses -> Total = 0

        // 4. Test GetRanking
        List<GuessRepository.UserRankingProjection> ranking = guessRepository.getRanking();

        assertThat(ranking).isNotEmpty();

        GuessRepository.UserRankingProjection aliceRank = null;
        GuessRepository.UserRankingProjection bobRank = null;
        GuessRepository.UserRankingProjection charlieRank = null;

        int aliceIndex = -1, bobIndex = -1, charlieIndex = -1;

        for (int i = 0; i < ranking.size(); i++) {
            GuessRepository.UserRankingProjection r = ranking.get(i);
            if (r.getUserId().equals(alice.getId())) {
                aliceRank = r;
                aliceIndex = i;
            }
            if (r.getUserId().equals(bob.getId())) {
                bobRank = r;
                bobIndex = i;
            }
            if (r.getUserId().equals(charlie.getId())) {
                charlieRank = r;
                charlieIndex = i;
            }
        }

        assertThat(aliceRank).isNotNull();
        assertThat(bobRank).isNotNull();
        assertThat(charlieRank).isNotNull();

        assertThat(aliceRank.getTotalScore()).isEqualTo(7);
        assertThat(bobRank.getTotalScore()).isEqualTo(2);
        assertThat(charlieRank.getTotalScore()).isEqualTo(0);

        assertThat(aliceIndex).isLessThan(bobIndex);
        assertThat(bobIndex).isLessThan(charlieIndex);
    }
}
