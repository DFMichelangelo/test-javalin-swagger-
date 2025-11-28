package com.octopus.dto;

import com.octopus.service.CorrelationIdProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response implements CorrelationIdProvider {
    private String correlationId;
    private String result;
    private boolean success;
}
