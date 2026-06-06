package com.vfdcb.bolao.championship.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompetitionMatchesResponse(List<MatchDto> matches) {
}
