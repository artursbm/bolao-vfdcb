package com.vfdcb.bolao.championship;

import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.MatchStatus;
import com.vfdcb.bolao.championship.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.ZonedDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ChampionshipIntegrationTestNullTeam {

    @Autowired
    private MatchRepository matchRepository;

    @Test
    public void testSaveAndFetchNullTeams() {
        Match match = new Match();
        match.setExternalId(9999L);
        match.setMatchTime(ZonedDateTime.now().plusDays(1));
        match.setStatus(MatchStatus.TIMED);
        match.setHomeTeam(null);
        match.setAwayTeam(null);

        matchRepository.save(match);

        Match fetched = matchRepository.findByExternalId(9999L).orElseThrow();
        assertNull(fetched.getHomeTeam());
        assertNull(fetched.getAwayTeam());
    }
}
