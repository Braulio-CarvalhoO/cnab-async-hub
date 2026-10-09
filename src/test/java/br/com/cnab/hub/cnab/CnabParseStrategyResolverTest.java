package br.com.cnab.hub.cnab;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CnabParseStrategyResolverTest {

    private final CnabParseStrategyResolver resolver = new CnabParseStrategyResolver(
            List.of(new Cnab240ParseStrategy(), new Cnab400ParseStrategy())
    );

    @Test
    void escolhe240() {
        byte[] file = CnabFileFactory.cnab240("001", "ACME", "11111111000191",
                List.of(new CnabFileFactory.Item("XPTO", "1", 100)));
        assertThat(resolver.resolve(file)).isInstanceOf(Cnab240ParseStrategy.class);
    }

    @Test
    void escolhe400() {
        byte[] file = CnabFileFactory.cnab400("001", "ACME", "11111111000191",
                List.of(new CnabFileFactory.Item("XPTO", "1", 100)));
        assertThat(resolver.resolve(file)).isInstanceOf(Cnab400ParseStrategy.class);
    }

    @Test
    void rejeitaLayoutDesconhecido() {
        assertThatThrownBy(() -> resolver.resolve("isto nao e cnab".getBytes()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Layout CNAB não reconhecido");
    }
}
