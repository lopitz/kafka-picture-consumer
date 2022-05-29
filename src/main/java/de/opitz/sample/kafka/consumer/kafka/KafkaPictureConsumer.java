package de.opitz.sample.kafka.consumer.kafka;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.*;

import de.opitz.sample.kafka.consumer.ui.Ui;

public class KafkaPictureConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaPictureConsumer.class);

    private final Consumer<String, byte[]> kafkaConsumer;
    private final String topic;
    private final Ui ui;
    private final AtomicBoolean kafkaConsumerClosed = new AtomicBoolean(false);

    public KafkaPictureConsumer(Consumer<String, byte[]> kafkaConsumer, String topic, Ui ui) {
        this.topic = topic;
        this.ui = ui;
        Objects.requireNonNull(kafkaConsumer);
        this.kafkaConsumer = kafkaConsumer;
        Executors
            .newSingleThreadExecutor()
            .submit(this::handleKafkaEvents);
    }

    private void handleKafkaEvents() {
        try {
            kafkaConsumer.subscribe(List.of(topic));
            handleKafkaEventsUnsafe();
        } catch (WakeupException e) {
            if (!kafkaConsumerClosed.get()) {
                throw e;
            }
        } finally {
            kafkaConsumer.close();
        }
    }

    private void handleKafkaEventsUnsafe() {
        while (!kafkaConsumerClosed.get()) {
            var records = kafkaConsumer.poll(Duration.ofSeconds(1));
            records.forEach(item -> ui.updateImage(extractImageName(item), item.value()));
            kafkaConsumer.commitSync();
        }
    }

    private String extractImageName(ConsumerRecord<String, byte[]> item) {
        var key = item.key();
        return key == null ? "" : key;
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Kafka connections");
        kafkaConsumerClosed.set(true);
        kafkaConsumer.wakeup();
    }
}
