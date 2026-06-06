package com.vfdcb.bolao.championship.client;

import com.vfdcb.bolao.championship.client.dto.CompetitionMatchesResponse;
import com.vfdcb.bolao.championship.client.dto.CompetitionTeamsResponse;
import com.vfdcb.bolao.championship.config.FootballDataProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FootballDataClient {

    private final RestClient restClient;
    private final FootballDataProperties properties;

    public FootballDataClient(FootballDataProperties properties, RestClient.Builder restClientBuilder, RateLimitInterceptor rateLimitInterceptor, com.fasterxml.jackson.databind.ObjectMapper defaultMapper) {
        this.properties = properties;
        
        com.fasterxml.jackson.databind.ObjectMapper footballMapper = defaultMapper.copy()
                .setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE);

        this.restClient = restClientBuilder
                .baseUrl("https://api.football-data.org/v4")
                .defaultHeader("X-Auth-Token", properties.getApiToken() != null ? properties.getApiToken() : "")
                .requestInterceptor(rateLimitInterceptor)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof org.springframework.http.converter.json.MappingJackson2HttpMessageConverter);
                    converters.add(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(footballMapper));
                })
                .build();
    }

    public CompetitionMatchesResponse getCompetitionMatches(Long competitionId) {
        return restClient.get()
                .uri("/competitions/{id}/matches", competitionId)
                .retrieve()
                .body(CompetitionMatchesResponse.class);
    }

    public CompetitionTeamsResponse getCompetitionTeams(Long competitionId) {
        return restClient.get()
                .uri("/competitions/{id}/teams", competitionId)
                .retrieve()
                .body(CompetitionTeamsResponse.class);
    }
}
