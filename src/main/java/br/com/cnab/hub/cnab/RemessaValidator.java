package br.com.cnab.hub.cnab;

import br.com.cnab.hub.cnab.model.Pagamento;
import br.com.cnab.hub.cnab.model.ParsedRemessa;
import br.com.cnab.hub.domain.LayoutType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RemessaValidator {

    public List<String> validate(ParsedRemessa remessa) {
        List<String> errors = new ArrayList<>();
        int expectedWidth = remessa.layout() == LayoutType.CNAB_240 ? 240 : 400;

        for (int i = 0; i < remessa.linhasOriginais().size(); i++) {
            String line = remessa.linhasOriginais().get(i);
            if (line.length() != expectedWidth) {
                errors.add("Linha %d: esperado %d posições, encontrado %d"
                        .formatted(i + 1, expectedWidth, line.length()));
            }
        }

        String header = remessa.linhasOriginais().getFirst();
        String trailer = remessa.linhasOriginais().getLast();
        if (remessa.layout() == LayoutType.CNAB_240) {
            if (header.charAt(7) != '0') {
                errors.add("Header de arquivo inválido: posição 8 deve ser 0");
            }
            if (trailer.charAt(7) != '9') {
                errors.add("Trailer de arquivo inválido: posição 8 deve ser 9");
            }
        } else {
            if (header.charAt(0) != '0') {
                errors.add("Header CNAB 400 inválido: posição 1 deve ser 0");
            }
            if (trailer.charAt(0) != '9') {
                errors.add("Trailer CNAB 400 inválido: posição 1 deve ser 9");
            }
        }

        if (remessa.banco().isBlank() || !remessa.banco().matches("\\d{3}")) {
            errors.add("Código do banco inválido");
        }
        if (remessa.cnpj().isBlank()) {
            errors.add("CNPJ da empresa ausente");
        }
        if (remessa.pagamentos().isEmpty()) {
            errors.add("Remessa sem pagamentos");
        }

        for (Pagamento pagamento : remessa.pagamentos()) {
            if (pagamento.valorCentavos() <= 0) {
                errors.add("Linha %d: valor deve ser maior que zero".formatted(pagamento.linha()));
            }
            if (pagamento.nomeFavorecido().isBlank()) {
                errors.add("Linha %d: nome do favorecido obrigatório".formatted(pagamento.linha()));
            }
        }

        if (remessa.quantidadeTrailer() > 0
                && remessa.quantidadeTrailer() != remessa.linhasOriginais().size()) {
            errors.add("Trailer inconsistente: quantidade %d, arquivo tem %d registros"
                    .formatted(remessa.quantidadeTrailer(), remessa.linhasOriginais().size()));
        }

        return List.copyOf(errors);
    }
}
