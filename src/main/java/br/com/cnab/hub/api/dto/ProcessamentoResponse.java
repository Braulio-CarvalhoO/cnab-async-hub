package br.com.cnab.hub.api.dto;

import br.com.cnab.hub.domain.IngestSource;
import br.com.cnab.hub.domain.LayoutType;
import br.com.cnab.hub.domain.ProcessamentoStatus;
import br.com.cnab.hub.persistence.ProcessamentoDocument;

import java.time.Instant;
import java.util.List;

public record ProcessamentoResponse(
        String correlationId,
        ProcessamentoStatus status,
        IngestSource source,
        LayoutType layout,
        String empresa,
        String cnpj,
        String banco,
        String inputKey,
        String outputKey,
        long valorTotalCentavos,
        int quantidadePagamentos,
        List<String> errors,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProcessamentoResponse from(ProcessamentoDocument doc) {
        return new ProcessamentoResponse(
                doc.getCorrelationId(),
                doc.getStatus(),
                doc.getSource(),
                doc.getLayout(),
                doc.getEmpresa(),
                doc.getCnpj(),
                doc.getBanco(),
                doc.getInputKey(),
                doc.getOutputKey(),
                doc.getValorTotalCentavos(),
                doc.getQuantidadePagamentos(),
                doc.getErrors(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
