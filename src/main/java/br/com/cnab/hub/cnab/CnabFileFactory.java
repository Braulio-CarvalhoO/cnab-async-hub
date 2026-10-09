package br.com.cnab.hub.cnab;

import br.com.cnab.hub.cnab.model.Pagamento;
import br.com.cnab.hub.domain.LayoutType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class CnabFileFactory {

    private CnabFileFactory() {
    }

    public record Item(String nome, String seuNumero, long valorCentavos) {
    }

    public static byte[] create(LayoutType layout, String banco, String empresa, String cnpj, List<Item> itens) {
        return layout == LayoutType.CNAB_400
                ? cnab400(banco, empresa, cnpj, itens)
                : cnab240(banco, empresa, cnpj, itens);
    }

    public static byte[] cnab240(String banco, String empresa, String cnpj, List<Item> itens) {
        DateTimeFormatter date = DateTimeFormatter.ofPattern("ddMMyyyy");
        DateTimeFormatter hour = DateTimeFormatter.ofPattern("HHmmss");
        List<String> lines = new ArrayList<>();

        lines.add(new FixedWidth(240)
                .put(1, CnabText.numeric(banco, 3))
                .put(4, "0000")
                .put(8, "0")
                .put(18, "2")
                .put(19, CnabText.numeric(cnpj, 14))
                .put(33, CnabText.alpha("CONV123", 20))
                .put(53, CnabText.numeric("1", 5))
                .put(59, CnabText.numeric("12345", 12))
                .put(73, CnabText.alpha(empresa, 30))
                .put(103, CnabText.alpha("BANCO DEMO", 30))
                .put(143, "1")
                .put(144, LocalDate.now().format(date))
                .put(152, LocalTime.now().format(hour))
                .put(158, CnabText.numeric("1", 6))
                .put(164, "084")
                .put(167, CnabText.numeric("1600", 5))
                .line());

        lines.add(new FixedWidth(240)
                .put(1, CnabText.numeric(banco, 3))
                .put(4, "0001")
                .put(8, "1")
                .put(9, "C")
                .put(10, "20")
                .put(18, "2")
                .put(19, CnabText.numeric(cnpj, 14))
                .put(73, CnabText.alpha(empresa, 30))
                .put(103, CnabText.alpha("PAGAMENTO FORNECEDORES", 40))
                .line());

        int seq = 1;
        for (Item item : itens) {
            lines.add(new FixedWidth(240)
                    .put(1, CnabText.numeric(banco, 3))
                    .put(4, "0001")
                    .put(8, "3")
                    .put(9, CnabText.numeric(Integer.toString(seq), 5))
                    .put(14, "A")
                    .put(21, CnabText.numeric(banco, 3))
                    .put(24, CnabText.numeric("1", 5))
                    .put(30, CnabText.numeric("99999", 12))
                    .put(44, CnabText.alpha(item.nome(), 30))
                    .put(74, CnabText.alpha(item.seuNumero(), 20))
                    .put(94, LocalDate.now().format(date))
                    .put(120, CnabText.money(item.valorCentavos(), 15))
                    .put(231, "00")
                    .line());
            seq++;
        }

        lines.add(new FixedWidth(240)
                .put(1, CnabText.numeric(banco, 3))
                .put(4, "0001")
                .put(8, "5")
                .line());

        int total = lines.size() + 1;
        lines.add(new FixedWidth(240)
                .put(1, CnabText.numeric(banco, 3))
                .put(4, "9999")
                .put(8, "9")
                .put(18, CnabText.numeric("1", 6))
                .put(24, CnabText.numeric(Integer.toString(total), 6))
                .line());

        return join(lines);
    }

    public static byte[] cnab400(String banco, String empresa, String cnpj, List<Item> itens) {
        DateTimeFormatter date = DateTimeFormatter.ofPattern("ddMMyy");
        List<String> lines = new ArrayList<>();

        lines.add(new FixedWidth(400)
                .put(1, "0")
                .put(2, "1")
                .put(3, "REMESSA")
                .put(4, CnabText.numeric(cnpj, 14))
                .put(27, CnabText.alpha(empresa, 20))
                .put(77, CnabText.numeric(banco, 3))
                .put(95, LocalDate.now().format(date))
                .put(101, CnabText.numeric("1", 7))
                .put(395, CnabText.numeric("1", 6))
                .line());

        int seq = 2;
        for (Item item : itens) {
            lines.add(new FixedWidth(400)
                    .put(1, "1")
                    .put(21, CnabText.numeric(banco, 3))
                    .put(24, CnabText.numeric("1", 5))
                    .put(29, CnabText.numeric("99999", 12))
                    .put(41, CnabText.alpha(item.nome(), 30))
                    .put(71, CnabText.alpha(item.seuNumero(), 10))
                    .put(121, LocalDate.now().format(date))
                    .put(127, CnabText.money(item.valorCentavos(), 13))
                    .put(395, CnabText.numeric(Integer.toString(seq), 6))
                    .line());
            seq++;
        }

        lines.add(new FixedWidth(400)
                .put(1, "9")
                .put(395, CnabText.numeric(Integer.toString(seq), 6))
                .line());
        return join(lines);
    }

    public static byte[] retorno240(List<String> originais, List<Pagamento> pagamentos) {
        List<String> lines = new ArrayList<>(originais);
        if (!lines.isEmpty()) {
            FixedWidth header = overlay(lines.getFirst());
            header.put(143, "2");
            lines.set(0, header.line());
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.length() == 240 && line.charAt(7) == '3' && line.charAt(13) == 'A') {
                FixedWidth record = overlay(line);
                String ocorrencia = "00";
                for (Pagamento pagamento : pagamentos) {
                    if (pagamento.linha() == i + 1 && pagamento.valorCentavos() <= 0) {
                        ocorrencia = "03";
                    }
                }
                record.put(231, ocorrencia);
                lines.set(i, record.line());
            }
        }
        return join(lines);
    }

    public static byte[] retorno400(List<String> originais) {
        List<String> lines = new ArrayList<>(originais);
        if (!lines.isEmpty()) {
            FixedWidth header = overlay(lines.getFirst());
            header.put(2, "2");
            lines.set(0, header.line());
        }
        return join(lines);
    }

    private static FixedWidth overlay(String line) {
        FixedWidth width = new FixedWidth(line.length());
        width.put(1, line);
        return width;
    }

    private static byte[] join(List<String> lines) {
        return String.join("\r\n", lines).getBytes(StandardCharsets.ISO_8859_1);
    }
}
