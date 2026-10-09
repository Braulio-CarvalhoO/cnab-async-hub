package br.com.cnab.hub.domain;

public enum ProcessamentoStatus {
    RECEIVED,
    DOWNLOADED,
    PARSED,
    VALIDATED,
    PERSISTED,
    RETORNO_GENERATED,
    DELIVERED,
    REJECTED
}
