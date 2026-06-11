package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.championship.model.Guess;
import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.MatchStatus;
import com.vfdcb.bolao.championship.model.ScoringConfig;
import com.vfdcb.bolao.championship.repository.GuessRepository;
import com.vfdcb.bolao.championship.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final GuessRepository guessRepository;
    private final ScoringConfig scoringConfig;

    public MatchService(MatchRepository matchRepository, GuessRepository guessRepository, ScoringConfig scoringConfig) {
        this.matchRepository = matchRepository;
        this.guessRepository = guessRepository;
        this.scoringConfig = scoringConfig;
    }

    @Transactional
    public boolean finalizeMatches() {
        boolean rankingChanged = false;
        var matches = matchRepository.findAllByStatus(MatchStatus.FINISHED);
        if (!matches.isEmpty()) {
            for (Match match : matches) {
                var homeScore = match.getHomeScore();
                var awayScore = match.getAwayScore();
                
                if (homeScore == null || awayScore == null) continue;

                List<Guess> guesses = guessRepository.findByMatchId(match.getId());

                for (Guess g : guesses) {
                    int pts = calculatePoints(g.getHomeScore(), g.getAwayScore(), homeScore, awayScore);
                    if (g.getPoints() == null || g.getPoints() != pts) {
                        g.setPoints(pts);
                        guessRepository.save(g);
                        rankingChanged = true;
                    }
                }
            }
        }
        return rankingChanged;
    }

    private int calculatePoints(int guessHome, int guessAway, int realHome, int realAway) {
        if (guessHome == realHome && guessAway == realAway) {
            return scoringConfig.getExact();
        }

        int guessDiff = guessHome - guessAway;
        int realDiff = realHome - realAway;

        if (realDiff == 0) {
            if (guessDiff == 0) {
                return scoringConfig.getDraw();
            }
            return 0;
        } else {
            int guessWinner = Integer.signum(guessDiff);
            int realWinner = Integer.signum(realDiff);

            if (guessWinner != realWinner) {
                return 0;
            }

            if (guessDiff == realDiff) {
                return scoringConfig.getWinnerDiff();
            }
            return scoringConfig.getWinner();
        }
    }
}
