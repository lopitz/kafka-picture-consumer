#!/bin/sh
java -Djava.awt.headless=false -jar build/libs/kafka-picture-consumer-0.1.0.jar --kafka.host=localhost:9092 --kafka.group.id=1 &
java -Djava.awt.headless=false -jar build/libs/kafka-picture-consumer-0.1.0.jar --kafka.host=localhost:9092 --kafka.group.id=1 &
java -Djava.awt.headless=false -jar build/libs/kafka-picture-consumer-0.1.0.jar --kafka.host=localhost:9092 --kafka.group.id=2 &