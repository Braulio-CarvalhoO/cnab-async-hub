package br.com.cnab.hub.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BucketInitializer.class);
    private final ObjectStorage objectStorage;

    public BucketInitializer(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    @Override
    public void run(ApplicationArguments args) {
        objectStorage.ensureBuckets();
        log.info("Buckets de entrada e saída conferidos");
    }
}
