package br.com.cnab.hub.cnab.model;

import br.com.cnab.hub.domain.LayoutType;

import java.util.List;

public record ParsedRemessa(
        LayoutType layout,
        String banco,
        String empresa,
        String cnpj,
        String dataGeracao,
        int sequencia,
        List<Pagamento> pagamentos,
        int quantidadeTrailer,
        long valorTotalCentavos,
        List<String> linhasOriginais
) {
}
