package kafka;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.base.Throwables;

import kafka.consumer.KafkaStream;
import kafka.message.MessageAndMetadata;

/**
 * Encapsulates the Kafka Scala stream in a Java stream to be able to work with...
 */
final class KafkaJavaStream<K, V> {

    private static final Logger logger = getLogger(KafkaJavaStream.class);

    private final LinkedBlockingQueue<MessageAndMetadata<K, V>> queue = new LinkedBlockingQueue<>();

    private KafkaJavaStream() {
    }

    public static <K, V> Stream<MessageAndMetadata<K, V>> of(KafkaStream<K, V> stream) {
        KafkaJavaStream<K, V> instance = new KafkaJavaStream<>();
        new Thread(() -> {
            stream.forEach(messageAndMetaData -> {
                instance.queue.offer(messageAndMetaData);
                String key = new String((byte[]) messageAndMetaData.key());
                if (key.contains("15")) {
                    logger.info("received image {} from kafka", key);
                }
            });
        }).start();
        return instance.createStream();
    }

    private Stream<MessageAndMetadata<K, V>> createStream() {
        return Stream.generate(() -> {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                Throwables.propagate(e);
            }
            return null;
        });
    }
}
