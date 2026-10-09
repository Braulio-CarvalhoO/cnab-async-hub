package br.com.cnab.hub.cnab;

import br.com.cnab.hub.cnab.model.ParsedRemessa;
import br.com.cnab.hub.domain.LayoutType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Cnab240ParseStrategyTest {

    private final Cnab240ParseStrategy strategy = new Cnab240ParseStrategy();

    @Test
    void parseiaRemessa240ComDoisPagamentos() {
        byte[] file = CnabFileFactory.cnab240(
                "237",
                "EMPRESA DEMO LTDA",
                "12345678000190",
                List.of(
                        new CnabFileFactory.Item("FORNECEDOR ALFA", "NF1001", 150_050),
                        new CnabFileFactory.Item("FORNECEDOR BETA", "NF1002", 99_990)
                )
        );

        assertThat(strategy.supports(file)).isTrue();
        ParsedRemessa parsed = strategy.parse(file);

        assertThat(parsed.layout()).isEqualTo(LayoutType.CNAB_240);
        assertThat(parsed.banco()).isEqualTo("237");
        assertThat(parsed.empresa()).isEqualTo("EMPRESA DEMO LTDA");
        assertThat(parsed.cnpj()).isEqualTo("12345678000190");
        assertThat(parsed.pagamentos()).hasSize(2);
        assertThat(parsed.pagamentos().getFirst().valorCentavos()).isEqualTo(150_050);
        assertThat(parsed.valorTotalCentavos()).isEqualTo(250_040);
        assertThat(parsed.linhasOriginais()).allMatch(line -> line.length() == 240);
    }
}
