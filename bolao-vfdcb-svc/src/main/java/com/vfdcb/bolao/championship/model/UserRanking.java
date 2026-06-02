package com.vfdcb.bolao.championship.model;

import java.util.UUID;

public record UserRanking(UUID userId, String userName, int totalScore) {
}
