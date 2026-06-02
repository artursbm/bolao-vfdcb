package com.vfdcb.bolao.championship.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        
        String availableHeader = response.getHeaders().getFirst("X-Requests-Available-Minute");
        if (availableHeader == null) {
            availableHeader = response.getHeaders().getFirst("X-RequestsAvailable");
        }
        
        String resetHeader = response.getHeaders().getFirst("X-RequestCounter-Reset");

        if (availableHeader != null && resetHeader != null) {
            try {
                int available = Integer.parseInt(availableHeader);
                int resetSeconds = Integer.parseInt(resetHeader);
                log.info("API Rate Limit: {} requests available. Resets in {} seconds.", available, resetSeconds);
                
                if (available <= 1 && resetSeconds > 0) {
                    log.warn("Rate limit approaching. Sleeping for {} seconds to avoid throttling...", resetSeconds + 1);
                    Thread.sleep((resetSeconds + 1) * 1000L);
                }
            } catch (Exception e) {
                log.error("Failed to parse rate limit headers", e);
            }
        }
        
        return response;
    }
}
