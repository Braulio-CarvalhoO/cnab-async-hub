package br.com.cnab.hub.cnab;

import br.com.cnab.hub.cnab.model.ParsedRemessa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RemessaValidatorTest {

    private final RemessaValidator validator = new RemessaValidator();
    private final Cnab240ParseStrategy parser = new Cnab240ParseStrategy();

    @Test
    void aceitaRemessaValida() {
        ParsedRemessa parsed = parser.parse(CnabFileFactory.cnab240(
                "237", "EMPRESA", "12345678000190",
                List.of(new CnabFileFactory.Item("FORNECEDOR", "NF1", 1_000))
        ));
        assertThat(validator.validate(parsed)).isEmpty();
    }

    @Test
    void rejeitaValorZero() {
        ParsedRemessa parsed = parser.parse(CnabFileFactory.cnab240(
                "237", "EMPRESA", "12345678000190",
                List.of(new CnabFileFactory.Item("FORNECEDOR", "NF1", 0))
        ));
        assertThat(validator.validate(parsed))
                .anyMatch(error -> error.contains("valor deve ser maior que zero"));
    }
}
