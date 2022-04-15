FROM openjdk:11-jdk-slim

COPY build/libs/kafka-processor-cosmos-block-*-standalone.jar /opt/kafka-processor-cosmos-block.jar

CMD ["java","-jar","/opt/kafka-processor-cosmos-block.jar"]
