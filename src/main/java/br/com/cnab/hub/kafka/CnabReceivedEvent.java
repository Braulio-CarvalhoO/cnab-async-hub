package br.com.cnab.hub.kafka;

import br.com.cnab.hub.domain.IngestSource;

import java.time.Instant;

public record CnabReceivedEvent(
        String eventId,
        String correlationId,
        IngestSource source,
        String bucket,
        String objectKey,
        long sizeBytes,
        String fileHash,
        Instant receivedAt,
        String forcedFailure
) {
}
