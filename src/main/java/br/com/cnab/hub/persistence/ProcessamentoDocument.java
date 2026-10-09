package br.com.cnab.hub.persistence;

import br.com.cnab.hub.domain.IngestSource;
import br.com.cnab.hub.domain.LayoutType;
import br.com.cnab.hub.domain.ProcessamentoStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("processamentos")
public class ProcessamentoDocument {

    @Id
    private String id;
    @Indexed(unique = true)
    private String correlationId;
    @Indexed(unique = true)
    private String fileHash;
    private IngestSource source;
    private ProcessamentoStatus status;
    private LayoutType layout;
    private String empresa;
    private String cnpj;
    private String banco;
    private String inputKey;
    private String outputKey;
    private long valorTotalCentavos;
    private int quantidadePagamentos;
    private List<String> errors = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public IngestSource getSource() {
        return source;
    }

    public void setSource(IngestSource source) {
        this.source = source;
    }

    public ProcessamentoStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessamentoStatus status) {
        this.status = status;
    }

    public LayoutType getLayout() {
        return layout;
    }

    public void setLayout(LayoutType layout) {
        this.layout = layout;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getInputKey() {
        return inputKey;
    }

    public void setInputKey(String inputKey) {
        this.inputKey = inputKey;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public void setOutputKey(String outputKey) {
        this.outputKey = outputKey;
    }

    public long getValorTotalCentavos() {
        return valorTotalCentavos;
    }

    public void setValorTotalCentavos(long valorTotalCentavos) {
        this.valorTotalCentavos = valorTotalCentavos;
    }

    public int getQuantidadePagamentos() {
        return quantidadePagamentos;
    }

    public void setQuantidadePagamentos(int quantidadePagamentos) {
        this.quantidadePagamentos = quantidadePagamentos;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
