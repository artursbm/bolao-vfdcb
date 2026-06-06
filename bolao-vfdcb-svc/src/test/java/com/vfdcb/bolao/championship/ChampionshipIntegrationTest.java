package com.vfdcb.bolao.championship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vfdcb.bolao.auth.repository.UserRepository;
import com.vfdcb.bolao.auth.service.AuthResult;
import com.vfdcb.bolao.auth.service.AuthService;
import com.vfdcb.bolao.championship.dto.FinalizeMatchRequest;
import com.vfdcb.bolao.championship.dto.SubmitGuessRequest;
import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.MatchStatus;
import com.vfdcb.bolao.championship.model.Team;
import com.vfdcb.bolao.championship.repository.GuessRepository;
import com.vfdcb.bolao.championship.repository.MatchRepository;
import com.vfdcb.bolao.championship.repository.TeamRepository;
import com.vfdcb.bolao.config.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class ChampionshipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GuessRepository guessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setup() {
        guessRepository.deleteAll();
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        // Setup user and auth
        AuthResult authResult = authService.signup("Champ User", "champ@test.com", "password123");
        Cookie sessionCookie = new Cookie("session", authResult.session().getId().toString()); // It gets translated by AuthInterceptor? Wait, AuthInterceptor uses cookieHelper.verify.
        // Actually we need a REAL signed cookie from the controller, but AuthService returns Session, we must sign it manually, OR mock interceptor OR login via MockMvc to get the cookie.
    }

    private Cookie getSignedCookie() throws Exception {
        String res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"champ@test.com\", \"password\":\"password123\"}"))
                .andReturn().getResponse().getCookie("session").getValue();
        return new Cookie("session", res);
    }

    private Match createMatch(int hoursOffset, MatchStatus status, String homeTeamName, String homeTeamCode, String awayTeamName, String awayTeamCode) {
        Team home = teamRepository.save(new Team(homeTeamName, homeTeamCode));
        Team away = teamRepository.save(new Team(awayTeamName, awayTeamCode));
        return matchRepository.save(new Match(home, away, ZonedDateTime.now().plusHours(hoursOffset), status));
    }

    @Test
    void testListMatches() throws Exception {
        createMatch(1, MatchStatus.TIMED, "Brazil", "BRA", "Argentina", "ARG");
        createMatch(-1, MatchStatus.FINISHED, "Germany", "GER", "France", "FRA");

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))); // Only TIMED / IN_PLAY
    }

    @Test
    void testListGuesses() throws Exception {
        Cookie authCookie = getSignedCookie();
        Match testMatch = createMatch(1, MatchStatus.TIMED, "Brazil", "BRA", "Argentina", "ARG");

        // Submit a guess first
        SubmitGuessRequest req = new SubmitGuessRequest(testMatch.getId(), 2, 1);
        mockMvc.perform(post("/api/guesses")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Get guesses and verify flat shape (unwrapped)
        mockMvc.perform(get("/api/guesses")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].home_score").value(2))
                .andExpect(jsonPath("$[0].away_score").value(1))
                .andExpect(jsonPath("$[0].match.id").value(testMatch.getId().toString()))
                .andExpect(jsonPath("$[0].match.home_team.name").value("Brazil"));
    }

    @Test
    void testSubmitGuessAndRankingFlow() throws Exception {
        Cookie authCookie = getSignedCookie();
        Match testMatch = createMatch(1, MatchStatus.TIMED, "Brazil", "BRA", "Argentina", "ARG");

        // 1. Submit Guess
        SubmitGuessRequest req = new SubmitGuessRequest(testMatch.getId(), 2, 1);
        mockMvc.perform(post("/api/guesses")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.home_score").value(2))
                .andExpect(jsonPath("$.away_score").value(1));

        // 2. Finalize Match
        FinalizeMatchRequest finReq = new FinalizeMatchRequest(testMatch.getId(), 2, 1);
        mockMvc.perform(post("/api/admin/match-results")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        // 3. Get Ranking
        mockMvc.perform(get("/api/ranking")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user_name").value("Champ User"))
                .andExpect(jsonPath("$[0].total_score").value(4)); // Exact score = 4 pts
    }
}
