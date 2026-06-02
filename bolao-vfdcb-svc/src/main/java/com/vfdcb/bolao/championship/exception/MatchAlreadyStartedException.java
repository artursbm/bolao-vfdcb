package com.vfdcb.bolao.championship.exception;

public class MatchAlreadyStartedException extends ChampionshipException {
    public MatchAlreadyStartedException() {
        super("match has already started, guesses are closed");
    }
}
