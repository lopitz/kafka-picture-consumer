package de.opitz.sample.kafka.consumer.config;

import java.util.Properties;

import de.opitz.sample.kafka.consumer.kafka.KafkaPictureConsumer;
import org.apache.kafka.clients.consumer.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import de.opitz.sample.kafka.consumer.ui.Ui;

@Configuration
public class KafkaConfiguration {
    @Value("${kafka.group.id}")
    private String groupId;

    @Value("${kafka.topic}")
    private String topic;

    @Value("${kafka.bootstrap.servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public KafkaPictureConsumer createKafkaTrackingEventConsumer(Ui ui) {
        return new KafkaPictureConsumer(new KafkaConsumer<>(createConsumerConfig()), topic, ui);
    }

    private Properties createConsumerConfig() {
        var props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return props;
    }
}
