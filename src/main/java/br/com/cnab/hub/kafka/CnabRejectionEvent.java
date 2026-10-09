package br.com.cnab.hub.kafka;

import java.time.Instant;
import java.util.List;

public record CnabRejectionEvent(
        String correlationId,
        String reason,
        List<String> errors,
        Instant occurredAt
) {
}
