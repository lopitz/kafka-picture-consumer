kafka-picture-consumer
======================

Simple sample Kafka consumer that reads image data from Kafka and show them on the screen. This application together with [kafka-picture-producer](../../../kafka-picture-producer) [build a demo](../../../kafka-picture-producer/blob/master/README.md#demo) to explain the differences between Queuing and Publish/Subscribe modes of [Kafka Messaging System](https://kafka.apache.org/) in a more entertaining way :smirk:

Preconditions
-------------
You'll need a running Kafka and Zookeeper. [You may find some information on creating the test setup in the kafka-picture-producer README.](../../../kafka-picture-producer/blob/master/README.md#test-setup)
Furthermore you gonna need a movie split into single frames (google for vlc scene video filter)...

Usage
-----

In the easiest way you simply run

    java -Djava.awt.headless=false -jar kafka-picture-consumer-0.1.0.jar --kafka.group.id=1

(make sure you went to build/libs folder before the execution)

but there are also some command line arguments

    java -Djava.awt.headless=false -jar kafka-picture-consumer-0.1.0.jar [--zookeeper.connect] [--kafka.topic] [--kafka.group.id]

| argument name             | argument value                                  | default        |
| ------------------------- | ----------------------------------------------- | -------------- |
| --zookeeper.connect       | zookeeper host                                  | localhost:2181 |
| --kafka.topic             | topic the images are published to               | images         |
| --kafka.group.id          | consumer group id                               |                |

These command lines could also be set in the [application.properties](src/main/resources/application.properties)

About the application
---------------------
This project uses [Spring Boot](http://projects.spring.io/spring-boot/) as application framework and Gradle to build. The application was written against Kafka 0.8.2.1. 

Build
-----
This project uses [Gradle](https://gradle.org/) for building the application. Simply run

    ./gradlew assemble

to build the executable jar file. You then will find it under build/libs.
