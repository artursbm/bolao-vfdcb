package com.vfdcb.bolao.championship.client.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompetitionTeamsResponse {
    private List<TeamDto> teams;

    public List<TeamDto> getTeams() { return teams; }
    public void setTeams(List<TeamDto> teams) { this.teams = teams; }
}
