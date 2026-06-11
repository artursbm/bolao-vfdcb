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

    public ChampionshipService(MatchRepository matchRepository,
                               GuessRepository guessRepository) {
        this.matchRepository = matchRepository;
        this.guessRepository = guessRepository;
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

}
