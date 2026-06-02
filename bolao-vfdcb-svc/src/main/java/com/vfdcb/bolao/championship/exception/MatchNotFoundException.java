package com.vfdcb.bolao.championship.exception;

public class MatchNotFoundException extends ChampionshipException {
    public MatchNotFoundException() {
        super("match not found");
    }
}
