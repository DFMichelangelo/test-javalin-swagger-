package com.octopus.dto;

import com.octopus.service.CorrelationIdProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Request implements CorrelationIdProvider {
    private String correlationId;
    private String payload;

    public Request(String payload) {
        this.correlationId = UUID.randomUUID().toString();
        this.payload = payload;
    }
}
