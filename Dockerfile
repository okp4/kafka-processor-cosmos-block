FROM openjdk:11-jdk-slim

COPY build/libs/kafka-processor-cosmos-block-*-standalone.jar /opt/kafka-connector-cosmos.jar
