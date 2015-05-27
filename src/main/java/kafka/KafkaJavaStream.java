package kafka;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import kafka.consumer.KafkaStream;
import kafka.message.MessageAndMetadata;

/**
 * Encapsulates the Kafka Scala stream in a Java stream to be able to work with...
 */
public final class KafkaJavaStream<K, V> {

    private final static Logger logger = LoggerFactory.getLogger(KafkaJavaStream.class);

    private final LinkedBlockingQueue<MessageAndMetadata<K, V>> queue = new LinkedBlockingQueue<>();

    private KafkaJavaStream() {
    }

    public static <K, V> Stream<MessageAndMetadata<K, V>> of(KafkaStream<K, V> stream) {
        KafkaJavaStream instance = new KafkaJavaStream();
        new Thread(() -> {
            stream.forEach(messageAndMetadata -> {
                instance.queue.offer(messageAndMetadata);
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
