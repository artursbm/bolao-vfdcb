package com.vfdcb.bolao.championship.client.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompetitionMatchesResponse {
    private List<MatchDto> matches;

    public List<MatchDto> getMatches() { return matches; }
    public void setMatches(List<MatchDto> matches) { this.matches = matches; }
}
