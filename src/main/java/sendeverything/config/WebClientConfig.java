package sendeverything.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://example.com") // Set a default base URL
                .defaultHeader("Header-Key", "Header-Value") // Set default headers if needed
                .build();
    }
}