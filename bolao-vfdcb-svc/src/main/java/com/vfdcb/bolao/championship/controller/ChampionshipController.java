package com.vfdcb.bolao.championship.controller;

import com.vfdcb.bolao.auth.model.User;
import com.vfdcb.bolao.championship.dto.SubmitGuessRequest;
import com.vfdcb.bolao.championship.model.Guess;
import com.vfdcb.bolao.championship.model.GuessWithMatch;
import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.UserRanking;
import com.vfdcb.bolao.championship.service.ChampionshipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChampionshipController {

    private final ChampionshipService championshipService;

    public ChampionshipController(ChampionshipService championshipService) {
        this.championshipService = championshipService;
    }

    @GetMapping("/matches")
    public List<Match> getMatches(@RequestParam(required = false) Integer daysFromNow) {
        return championshipService.listUpcomingMatches();
    }

    @GetMapping("/ranking")
    public List<UserRanking> getRanking() {
        return championshipService.getRanking();
    }

    @GetMapping("/guesses")
    public List<GuessWithMatch> getGuesses(HttpServletRequest request) {
        User user = getUser(request);
        return championshipService.getUserGuesses(user.getId());
    }

    @PostMapping("/guesses")
    public Guess submitGuess(@Valid @RequestBody SubmitGuessRequest req, HttpServletRequest request) {
        User user = getUser(request);
        return championshipService.submitGuess(user.getId(), req.matchId(), req.homeScore(), req.awayScore());
    }

    private User getUser(HttpServletRequest request) {
        User user = (User) request.getAttribute("user");
        if (user == null) {
            throw new com.vfdcb.bolao.auth.exception.AuthException("Unauthorized");
        }
        return user;
    }
}
