package com.vfdcb.bolao.championship.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitGuessRequest(
        @NotNull UUID matchId,
        @Min(0) int homeScore,
        @Min(0) int awayScore
) {
}
