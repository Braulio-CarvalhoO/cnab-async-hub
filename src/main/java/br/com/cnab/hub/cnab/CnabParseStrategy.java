package br.com.cnab.hub.cnab;

import br.com.cnab.hub.cnab.model.ParsedRemessa;
import br.com.cnab.hub.domain.LayoutType;

public interface CnabParseStrategy {

    LayoutType layout();

    boolean supports(byte[] content);

    ParsedRemessa parse(byte[] content);
}
