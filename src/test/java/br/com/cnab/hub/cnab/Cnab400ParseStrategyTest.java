package br.com.cnab.hub.cnab;

import br.com.cnab.hub.cnab.model.ParsedRemessa;
import br.com.cnab.hub.domain.LayoutType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Cnab400ParseStrategyTest {

    private final Cnab400ParseStrategy strategy = new Cnab400ParseStrategy();

    @Test
    void parseiaRemessa400() {
        byte[] file = CnabFileFactory.cnab400(
                "341",
                "EMPRESA DEMO LTDA",
                "12345678000190",
                List.of(new CnabFileFactory.Item("FORNECEDOR GAMA", "BOL22", 10_000))
        );

        assertThat(strategy.supports(file)).isTrue();
        ParsedRemessa parsed = strategy.parse(file);

        assertThat(parsed.layout()).isEqualTo(LayoutType.CNAB_400);
        assertThat(parsed.banco()).isEqualTo("341");
        assertThat(parsed.pagamentos()).hasSize(1);
        assertThat(parsed.pagamentos().getFirst().nomeFavorecido()).isEqualTo("FORNECEDOR GAMA");
        assertThat(parsed.pagamentos().getFirst().valorCentavos()).isEqualTo(10_000);
        assertThat(parsed.linhasOriginais()).allMatch(line -> line.length() == 400);
    }
}
