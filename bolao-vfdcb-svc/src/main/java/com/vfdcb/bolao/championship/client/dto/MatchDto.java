package com.vfdcb.bolao.championship.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchDto(Long id, ZonedDateTime utcDate, String status, TeamDto homeTeam, TeamDto awayTeam,
                       ScoreDto score, String stage, ZonedDateTime lastUpdated) {
}
