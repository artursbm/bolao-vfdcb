package com.vfdcb.bolao.championship.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.scoring")
public class ScoringConfig {

    private int exact;
    private int winnerDiff;
    private int winner;
    private int draw;

    public int getExact() {
        return exact;
    }

    public void setExact(int exact) {
        this.exact = exact;
    }

    public int getWinnerDiff() {
        return winnerDiff;
    }

    public void setWinnerDiff(int winnerDiff) {
        this.winnerDiff = winnerDiff;
    }

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }

    public int getDraw() {
        return draw;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }
}
