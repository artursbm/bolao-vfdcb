package com.vfdcb.bolao.championship.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreDto {
    private ScoreDetail fullTime;

    public ScoreDetail getFullTime() {
        return fullTime;
    }

    public void setFullTime(ScoreDetail fullTime) {
        this.fullTime = fullTime;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreDetail {
        private Integer home;
        private Integer away;

        public Integer getHome() {
            return home;
        }

        public void setHome(Integer home) {
            this.home = home;
        }

        public Integer getAway() {
            return away;
        }

        public void setAway(Integer away) {
            this.away = away;
        }
    }
}
