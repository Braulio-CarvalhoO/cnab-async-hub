package br.com.cnab.hub.cnab;

import br.com.cnab.hub.cnab.model.Pagamento;
import br.com.cnab.hub.cnab.model.ParsedRemessa;
import br.com.cnab.hub.domain.LayoutType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Cnab240ParseStrategy implements CnabParseStrategy {

    @Override
    public LayoutType layout() {
        return LayoutType.CNAB_240;
    }

    @Override
    public boolean supports(byte[] content) {
        List<String> lines = CnabLines.split(content);
        return !lines.isEmpty() && CnabLines.dominantWidth(lines) == 240;
    }

    @Override
    public ParsedRemessa parse(byte[] content) {
        List<String> lines = CnabLines.split(content);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CNAB 240 vazio");
        }

        String header = lines.getFirst();
        String trailer = lines.getLast();

        List<Pagamento> pagamentos = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.length() < 240) {
                continue;
            }
            if (line.charAt(7) == '3' && line.charAt(13) == 'A') {
                pagamentos.add(parseSegmentoA(i + 1, line));
            }
        }

        int quantidadeTrailer = parseInt(CnabText.slice(trailer, 24, 29));
        return new ParsedRemessa(
                LayoutType.CNAB_240,
                CnabText.slice(header, 1, 3),
                CnabText.slice(header, 73, 102),
                CnabText.slice(header, 19, 32),
                CnabText.slice(header, 144, 151),
                parseInt(CnabText.slice(header, 158, 163)),
                List.copyOf(pagamentos),
                quantidadeTrailer,
                pagamentos.stream().mapToLong(Pagamento::valorCentavos).sum(),
                lines
        );
    }

    private Pagamento parseSegmentoA(int linha, String record) {
        return new Pagamento(
                linha,
                CnabText.slice(record, 21, 23),
                CnabText.slice(record, 24, 28),
                CnabText.slice(record, 30, 41),
                CnabText.slice(record, 44, 73),
                CnabText.slice(record, 74, 93),
                CnabText.slice(record, 94, 101),
                parseLong(CnabText.slice(record, 120, 134)),
                CnabText.slice(record, 231, 240)
        );
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value.replaceAll("\\D", "").isBlank() ? "0" : value.replaceAll("\\D", ""));
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? 0 : Long.parseLong(digits);
    }
}
