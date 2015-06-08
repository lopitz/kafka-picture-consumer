package kafka;

import java.util.Spliterators;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.consumer.KafkaStream;
import kafka.message.MessageAndMetadata;

/**
 * Encapsulates the Kafka Scala stream in a Java stream to be able to work with...
 */
final class KafkaJavaStream<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaJavaStream.class);
    private static final Executor KAFKA_EVENT_HANDLER_THREAD_EXECUTOR = Executors.newCachedThreadPool();

    private final LinkedBlockingQueue<BufferElement<K, V>> queue = new LinkedBlockingQueue<>();
    private final KafkaStream<K, V> kafkaStream;

    private KafkaJavaStream(KafkaStream<K, V> stream) {
        kafkaStream = stream;
    }

    public static <K, V> Stream<MessageAndMetadata<K, V>> of(KafkaStream<K, V> stream) {
        KafkaJavaStream<K, V> instance = new KafkaJavaStream<>(stream);
        instance.startHandlingKafkaEvents();
        return instance.createStream();
    }

    private void startHandlingKafkaEvents() {
        KAFKA_EVENT_HANDLER_THREAD_EXECUTOR.execute(() -> {
            try {
                kafkaStream.forEach(messageAndMetadata -> queue.offer(BufferElement.of(messageAndMetadata)));
            } catch (Exception e) {
                logger.warn("Error retrieving event from Kafka.", e);
                queue.offer(BufferElement.createStopElement());
            }
        });
    }

    private Stream<MessageAndMetadata<K, V>> createStream() {
        return StreamSupport.stream(new KafkaStreamSpliterator<>(queue), false);
    }

    private static class BufferElement<K, V> {

        private final MessageAndMetadata<K, V> messageAndMetadata;
        private final boolean stopElement;

        private BufferElement(MessageAndMetadata<K, V> messageAndMetadata, boolean stopElement) {
            this.messageAndMetadata = messageAndMetadata;
            this.stopElement = stopElement || messageAndMetadata == null;
        }

        public static <K, V> BufferElement<K, V> createStopElement() {
            return new BufferElement<>(null, true);
        }

        public static <K, V> BufferElement<K, V> of(MessageAndMetadata<K, V> messageAndMetadata) {
            return new BufferElement<>(messageAndMetadata, false);
        }

        public boolean isStopElement() {
            return stopElement;
        }
    }

    private static class KafkaStreamSpliterator<K, V>
            extends Spliterators.AbstractSpliterator<MessageAndMetadata<K, V>> {

        public static final int NO_SIZE_ESTIMATION = -1;
        public static final int NO_ADDITIONAL_CHARACTERISTICS = 0;

        private final LinkedBlockingQueue<BufferElement<K, V>> queue;

        private KafkaStreamSpliterator(LinkedBlockingQueue<BufferElement<K, V>> queue) {
            super(NO_SIZE_ESTIMATION, NO_ADDITIONAL_CHARACTERISTICS);
            this.queue = queue;
        }

        @Override
        public boolean tryAdvance(Consumer<? super MessageAndMetadata<K, V>> action) {
            BufferElement<K, V> bufferElement;
            try {
                bufferElement = queue.take();
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for Kafka events.");
                return false;
            }
            if (bufferElement.isStopElement()) {
                logger.info("Found stop element in Kafka stream.");
                return false;
            }
            action.accept(bufferElement.messageAndMetadata);
            return true;
        }
    }
}
