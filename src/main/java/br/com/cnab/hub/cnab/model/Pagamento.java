package br.com.cnab.hub.cnab.model;

public record Pagamento(
        int linha,
        String bancoFavorecido,
        String agencia,
        String conta,
        String nomeFavorecido,
        String seuNumero,
        String dataPagamento,
        long valorCentavos,
        String ocorrencia
) {
}
