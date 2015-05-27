package config;

import java.util.Properties;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;

import kafka.KafkaPictureConsumer;
import kafka.consumer.ConsumerConfig;
import ui.Ui;

@Configuration
public class KafkaConfiguration {

    @Value("${zookeeper.connect}")
    private String zookeeper;

    @Value("${kafka.group.id}")
    private String groupId;

    @Value("${kafka.topic}")
    private String topic;

    @Bean
    @Inject
    public KafkaPictureConsumer createKafkaTrackingEventConsumer(MetricRegistry metricRegistry, Ui ui) {
        return new KafkaPictureConsumer(kafka.consumer.Consumer.createJavaConsumerConnector(createConsumerConfig()),
                topic, metricRegistry, ui);
    }

    private ConsumerConfig createConsumerConfig() {
        Properties props = new Properties();
        props.put("zookeeper.connect", zookeeper);
        props.put("group.id", groupId);
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        return new ConsumerConfig(props);
    }
}
