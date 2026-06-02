package com.vfdcb.bolao.championship.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDto {
    private Long id;
    private ZonedDateTime utcDate;
    private String status;
    private TeamDto homeTeam;
    private TeamDto awayTeam;
    private ScoreDto score;
    private String stage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ZonedDateTime getUtcDate() { return utcDate; }
    public void setUtcDate(ZonedDateTime utcDate) { this.utcDate = utcDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public TeamDto getHomeTeam() { return homeTeam; }
    public void setHomeTeam(TeamDto homeTeam) { this.homeTeam = homeTeam; }

    public TeamDto getAwayTeam() { return awayTeam; }
    public void setAwayTeam(TeamDto awayTeam) { this.awayTeam = awayTeam; }

    public ScoreDto getScore() { return score; }
    public void setScore(ScoreDto score) { this.score = score; }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}
