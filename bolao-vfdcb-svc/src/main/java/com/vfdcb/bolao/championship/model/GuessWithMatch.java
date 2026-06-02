package com.vfdcb.bolao.championship.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record GuessWithMatch(
    @JsonUnwrapped Guess guess,
    Match match
) {}
