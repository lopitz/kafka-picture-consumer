kafka-picture-consumer
======================

Simple sample Kafka consumer that reads image data from Kafka and show them on the screen. 
This application together with [kafka-picture-producer][1] [builds a demo][2] to explain the 
differences between Queuing and Publish/Subscribe modes of [Kafka Messaging System](https://kafka.apache.org/) in a more entertaining way :smirk:

Preconditions
-------------
You'll need a running Apaches Kafka. [You may find some information on creating the test setup in the kafka-picture-producer README.][3].

Usage
-----

In the easiest way you simply de.opitz.sample.kafka.consumer.Application

    java -Djava.awt.headless=false -jar kafka-picture-consumer-2.0.0.jar --kafka.group.id=1

(make sure you went to `target` folder before the execution)

but there are also some command line arguments

    java -Djava.awt.headless=false -jar kafka-picture-consumer-2.0.0.jar [--kafka.topic] [--kafka.group.id]

| argument name             | argument value                               | default        |
|---------------------------|----------------------------------------------|----------------|
| --kafka.topic             | topic the images are published to            | images         |
| --kafka.group.id          | consumer group id                            |                |
| --kafka.bootstrap.servers | url where the Kafka broker(s) can be reached | localhost:9092 |
| --ui.position.left        | horizontal starting position of the window   | 0              |
| --ui.position.top         | vertical starting position of the window     | 0              |


These command lines could also be set in the [application.yml](src/main/resources/application.yml)

About the application
---------------------
This project uses [Spring Boot](http://projects.spring.io/spring-boot/) as application framework and Maven to build. The application was written against 
Kafka 3.2. 

Build
-----
This project uses [Maven](https://maven.apache.org/) for building the application. Simply run

    ./mvnw clean install 

to build the executable jar file. You then will find it under target.

[1]: ../../../kafka-picture-producer 
[2]: ../../../kafka-picture-producer/blob/main/README.md#demo
[3]: ../../../kafka-picture-producer/blob/master/README.md#test-setup
