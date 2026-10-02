package br.com.fiap.phase4.exec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {"br.com.fiap.phase4"})
@EnableMongoAuditing
@EnableKafka
public class ExecApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecApplication.class, args);
    }
}
