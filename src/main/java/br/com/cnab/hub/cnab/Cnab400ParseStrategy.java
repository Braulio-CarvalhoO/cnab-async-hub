package br.com.cnab.hub.cnab;

import br.com.cnab.hub.cnab.model.Pagamento;
import br.com.cnab.hub.cnab.model.ParsedRemessa;
import br.com.cnab.hub.domain.LayoutType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Cnab400ParseStrategy implements CnabParseStrategy {

    @Override
    public LayoutType layout() {
        return LayoutType.CNAB_400;
    }

    @Override
    public boolean supports(byte[] content) {
        List<String> lines = CnabLines.split(content);
        return !lines.isEmpty() && CnabLines.dominantWidth(lines) == 400;
    }

    @Override
    public ParsedRemessa parse(byte[] content) {
        List<String> lines = CnabLines.split(content);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CNAB 400 vazio");
        }

        String header = lines.getFirst();
        String trailer = lines.getLast();
        List<Pagamento> pagamentos = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.length() >= 400 && line.charAt(0) == '1') {
                pagamentos.add(parseDetail(i + 1, line));
            }
        }

        int quantidadeTrailer = parseInt(CnabText.slice(trailer, 395, 400));
        return new ParsedRemessa(
                LayoutType.CNAB_400,
                CnabText.slice(header, 77, 79),
                CnabText.slice(header, 27, 46),
                CnabText.slice(header, 4, 17),
                CnabText.slice(header, 95, 100),
                parseInt(CnabText.slice(header, 101, 107)),
                List.copyOf(pagamentos),
                quantidadeTrailer,
                pagamentos.stream().mapToLong(Pagamento::valorCentavos).sum(),
                lines
        );
    }

    private Pagamento parseDetail(int linha, String record) {
        return new Pagamento(
                linha,
                CnabText.slice(record, 21, 23),
                CnabText.slice(record, 24, 28),
                CnabText.slice(record, 29, 40),
                CnabText.slice(record, 41, 70),
                CnabText.slice(record, 71, 80),
                CnabText.slice(record, 121, 126),
                parseLong(CnabText.slice(record, 127, 139)),
                CnabText.slice(record, 396, 400)
        );
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? 0 : Integer.parseInt(digits);
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? 0 : Long.parseLong(digits);
    }
}
