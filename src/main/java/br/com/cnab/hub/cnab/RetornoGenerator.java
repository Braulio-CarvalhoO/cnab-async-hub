package br.com.cnab.hub.cnab;

import br.com.cnab.hub.cnab.model.ParsedRemessa;
import br.com.cnab.hub.domain.LayoutType;
import org.springframework.stereotype.Component;

@Component
public class RetornoGenerator {

    public byte[] generate(ParsedRemessa remessa) {
        if (remessa.layout() == LayoutType.CNAB_400) {
            return CnabFileFactory.retorno400(remessa.linhasOriginais());
        }
        return CnabFileFactory.retorno240(remessa.linhasOriginais(), remessa.pagamentos());
    }
}
