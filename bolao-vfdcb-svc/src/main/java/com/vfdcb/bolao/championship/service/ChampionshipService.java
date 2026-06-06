package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.championship.exception.MatchAlreadyStartedException;
import com.vfdcb.bolao.championship.exception.MatchNotFoundException;
import com.vfdcb.bolao.championship.model.*;
import com.vfdcb.bolao.championship.repository.GuessRepository;
import com.vfdcb.bolao.championship.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChampionshipService {

    private final MatchRepository matchRepository;
    private final GuessRepository guessRepository;
    private final ScoringConfig scoringConfig;

    public ChampionshipService(MatchRepository matchRepository,
                               GuessRepository guessRepository,
                               ScoringConfig scoringConfig) {
        this.matchRepository = matchRepository;
        this.guessRepository = guessRepository;
        this.scoringConfig = scoringConfig;
    }

    public List<Match> listUpcomingMatches() {
        return matchRepository
                .findByStatusInAndMatchTimeAfterOrderByMatchTimeAsc(List.of(MatchStatus.TIMED, MatchStatus.IN_PLAY, MatchStatus.FINISHED),
                        ZonedDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()));
    }

    public List<UserRanking> getRanking() {
        return guessRepository.getRanking().stream()
                .map(p -> new UserRanking(p.getUserId(), p.getUserName(), p.getTotalScore()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Guess submitGuess(UUID userId, UUID matchId, int homeScore, int awayScore) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(MatchNotFoundException::new);

        if (!ZonedDateTime.now().isBefore(match.getMatchTime()) || match.getStatus() != MatchStatus.TIMED) {
            throw new MatchAlreadyStartedException();
        }

        Guess guess = guessRepository.findByUserIdAndMatchId(userId, matchId)
                .orElse(new Guess(userId, matchId, homeScore, awayScore));

        guess.setHomeScore(homeScore);
        guess.setAwayScore(awayScore);

        return guessRepository.save(guess);
    }

    public List<GuessWithMatch> getUserGuesses(UUID userId) {
        List<Match> allMatches = matchRepository.findAllByOrderByMatchTimeAsc();

        return allMatches.stream().map(match -> {
            Optional<Guess> guessOpt = guessRepository.findByUserIdAndMatchId(userId, match.getId());
            Guess guess = guessOpt.orElseGet(() -> {
                Guess g = new Guess(userId, match.getId(), null, null);
                g.setId(UUID.randomUUID());
                return g;
            });
            return new GuessWithMatch(guess, match);
        }).collect(Collectors.toList());
    }

    @Transactional
    public Match finalizeMatch(UUID matchId, int homeScore, int awayScore) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(MatchNotFoundException::new);

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setStatus(MatchStatus.FINISHED);
        match = matchRepository.save(match);

        List<Guess> guesses = guessRepository.findByMatchId(matchId);
        for (Guess g : guesses) {
            int pts = calculatePoints(g.getHomeScore(), g.getAwayScore(), homeScore, awayScore);
            g.setPoints(pts);
            guessRepository.save(g);
        }

        return match;
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
