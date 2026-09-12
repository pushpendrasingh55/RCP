package com.rcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// A single shared ObjectMapper bean, reused both by Spring MVC's request/response (de)serialization
// and by our own startup-time config loaders (JsonRateConfigurationProvider, WorkflowService),
// so LocalDate/Instant handling is configured in exactly one place.
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
