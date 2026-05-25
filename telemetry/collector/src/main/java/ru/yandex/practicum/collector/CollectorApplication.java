package ru.yandex.practicum.collector;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.kafka.clients.producer.Producer;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CollectorApplication {

    @Autowired
    private Producer<String, SpecificRecordBase> kafkaProducer;

    public static void main(String[] args) {
        SpringApplication.run(CollectorApplication.class, args);
    }

    @PreDestroy
    public void onShutdown() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }
}