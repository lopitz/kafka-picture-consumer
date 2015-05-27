package config;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

@Configuration
public class MetricsConfiguration {

    @Bean
    public MetricRegistry createMetricRegistry() {
        return new MetricRegistry();
    }


    @Inject
    public void configureReporters(MetricRegistry metricRegistry) {
        ConsoleReporter.forRegistry(metricRegistry).build().start(1, TimeUnit.MINUTES);
    }
}
