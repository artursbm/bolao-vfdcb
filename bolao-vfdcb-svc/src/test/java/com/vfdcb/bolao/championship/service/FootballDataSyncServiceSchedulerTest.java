package com.vfdcb.bolao.championship.service;

import com.vfdcb.bolao.championship.client.FootballDataClient;
import com.vfdcb.bolao.championship.client.dto.CompetitionMatchesResponse;
import com.vfdcb.bolao.championship.config.FootballDataProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Collections;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import org.springframework.context.annotation.Import;
import com.vfdcb.bolao.config.TestcontainersConfiguration;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.scheduling.enabled=true",
        "football-data.sync-delay=1000" // 1 second delay for testing
})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public class FootballDataSyncServiceSchedulerTest {

    @MockitoBean
    private FootballDataClient footballDataClient;

    @MockitoBean
    private FootballDataProperties properties;

    @Test
    void testScheduledSyncMatches() {
        when(properties.getCompetitionId()).thenReturn(2000L);
        when(footballDataClient.getCompetitionMatches(2000L))
                .thenReturn(new CompetitionMatchesResponse(Collections.emptyList()));

        // Assert that the client method was called at least 2 times within 5 seconds.
        // This proves the scheduler is successfully triggering the periodic execution.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(footballDataClient, atLeast(2)).getCompetitionMatches(2000L);
        });
    }
}
