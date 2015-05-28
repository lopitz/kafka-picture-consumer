package kafka;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;

import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import ui.Ui;

public class KafkaPictureConsumer {

    private final static Logger logger = LoggerFactory.getLogger(KafkaPictureConsumer.class);

    private final ConsumerConnector consumerConnector;
    private final String topic;
    private final MetricRegistry metricRegistry;
    private final List<Stream<MessageAndMetadata<byte[], byte[]>>> kafkaStreams = new ArrayList<>();
    private final Ui ui;

    public KafkaPictureConsumer(ConsumerConnector consumerConnector, String topic, MetricRegistry metricRegistry, Ui ui) {
        this.topic = topic;
        this.metricRegistry = metricRegistry;
        this.ui = ui;
        checkNotNull(consumerConnector);
        this.consumerConnector = consumerConnector;
        createKafkaStreams();
        handleKafkaEvents();
    }

    private void createKafkaStreams() {
        consumerConnector.createMessageStreams(ImmutableMap.of(topic, 1))
                .get(topic)
                .forEach(stream -> kafkaStreams.add(KafkaJavaStream.of(stream)));
    }

    private void handleKafkaEvents() {
        kafkaStreams.forEach(stream -> new Thread(() -> {
            stream.forEach(messageAndMetadata -> {
                metricRegistry.counter(
                        MetricRegistry.name("outbound", "kafka", "consuming")).inc();
                ui.updateImage(new String(messageAndMetadata.key()), messageAndMetadata.message());
            });
        }).start());
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Kafka connections");
    }
}
