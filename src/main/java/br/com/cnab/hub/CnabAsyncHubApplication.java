package br.com.cnab.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CnabAsyncHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CnabAsyncHubApplication.class, args);
    }
}
