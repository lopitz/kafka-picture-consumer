#!/bin/sh
java -Djava.awt.headless=false -jar target/kafka-picture-consumer-2.0.0.jar --ui.position.left=0 --kafka.group.id=1 &
java -Djava.awt.headless=false -jar target/kafka-picture-consumer-2.0.0.jar --ui.position.left=400 --kafka.group.id=1 &
java -Djava.awt.headless=false -jar target/kafka-picture-consumer-2.0.0.jar --ui.position.top=350 --kafka.group.id=2 &
