package br.com.cnab.hub.cnab;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CnabParseStrategyResolver {

    private final List<CnabParseStrategy> strategies;

    public CnabParseStrategyResolver(List<CnabParseStrategy> strategies) {
        this.strategies = strategies;
    }

    public CnabParseStrategy resolve(byte[] content) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(content))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Layout CNAB não reconhecido. Esperado 240 ou 400 posições por linha."));
    }
}
