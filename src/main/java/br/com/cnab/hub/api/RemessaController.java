package br.com.cnab.hub.api;

import br.com.cnab.hub.api.dto.ProcessamentoResponse;
import br.com.cnab.hub.cnab.CnabFileFactory;
import br.com.cnab.hub.config.StorageProperties;
import br.com.cnab.hub.domain.IngestSource;
import br.com.cnab.hub.domain.LayoutType;
import br.com.cnab.hub.ingest.RemessaIngestService;
import br.com.cnab.hub.persistence.ProcessamentoDocument;
import br.com.cnab.hub.persistence.ProcessamentoRepository;
import br.com.cnab.hub.storage.ObjectStorage;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/remessas")
public class RemessaController {

    private final RemessaIngestService ingestService;
    private final ProcessamentoRepository processamentoRepository;
    private final ObjectStorage objectStorage;
    private final StorageProperties storageProperties;

    public RemessaController(
            RemessaIngestService ingestService,
            ProcessamentoRepository processamentoRepository,
            ObjectStorage objectStorage,
            StorageProperties storageProperties
    ) {
        this.ingestService = ingestService;
        this.processamentoRepository = processamentoRepository;
        this.objectStorage = objectStorage;
        this.storageProperties = storageProperties;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessamentoResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "API") IngestSource source,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestHeader(value = "X-Demo-Fail", required = false) String demoFail
    ) throws Exception {
        ProcessamentoDocument doc = ingestService.ingest(
                file.getBytes(),
                file.getOriginalFilename(),
                source,
                correlationId,
                demoFail
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ProcessamentoResponse.from(doc));
    }

    @PostMapping("/demo")
    public ResponseEntity<ProcessamentoResponse> demo(
            @RequestParam(defaultValue = "CNAB_240") LayoutType layout,
            @RequestHeader(value = "X-Demo-Fail", required = false) String demoFail
    ) {
        byte[] content = CnabFileFactory.create(
                layout,
                "237",
                "EMPRESA DEMO LTDA",
                "12345678000190",
                List.of(
                        new CnabFileFactory.Item("FORNECEDOR ALFA", "NF1001", 150_050),
                        new CnabFileFactory.Item("FORNECEDOR BETA", "NF1002", 99_990)
                )
        );
        ProcessamentoDocument doc = ingestService.ingest(
                content,
                "demo-" + layout.name().toLowerCase() + ".rem",
                IngestSource.FRONTEND,
                null,
                demoFail
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ProcessamentoResponse.from(doc));
    }

    @GetMapping
    public List<ProcessamentoResponse> list() {
        return processamentoRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(ProcessamentoResponse::from)
                .toList();
    }

    @GetMapping("/{correlationId}")
    public ResponseEntity<ProcessamentoResponse> get(@PathVariable String correlationId) {
        return processamentoRepository.findByCorrelationId(correlationId)
                .map(doc -> ResponseEntity.ok(ProcessamentoResponse.from(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{correlationId}/retorno")
    public ResponseEntity<byte[]> downloadRetorno(@PathVariable String correlationId) {
        return processamentoRepository.findByCorrelationId(correlationId)
                .filter(doc -> doc.getOutputKey() != null)
                .map(doc -> {
                    byte[] body = objectStorage.download(storageProperties.outputBucket(), doc.getOutputKey());
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    headers.setContentDisposition(ContentDisposition.attachment()
                            .filename("retorno-" + correlationId + ".ret")
                            .build());
                    return new ResponseEntity<>(body, headers, HttpStatus.OK);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
