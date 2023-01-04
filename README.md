# Kafka Processor CØSMOS-Block

> A Kafka Streams Processor to unwrap CØSMOS blocks into CØSMOS transactions powered by Quarkus.

[![version](https://img.shields.io/github/v/release/okp4/kafka-processor-cosmos-block?style=for-the-badge&logo=github)](https://github.com/okp4/kafka-processor-cosmos-block/releases)
[![build](https://img.shields.io/github/actions/workflow/status/okp4/kafka-processor-cosmos-block/build.yml?branch=main&label=build&style=for-the-badge&logo=github)](https://github.com/okp4/kafka-processor-cosmos-block/actions/workflows/build.yml)
[![lint](https://img.shields.io/github/actions/workflow/status/okp4/kafka-processor-cosmos-block/lint.yml?branch=main&label=lint&style=for-the-badge&logo=github)](https://github.com/okp4/kafka-processor-cosmos-block/actions/workflows/lint.yml)
[![test](https://img.shields.io/github/actions/workflow/status/okp4/kafka-processor-cosmos-block/test.yml?branch=main&label=test&style=for-the-badge&logo=github)](https://github.com/okp4/kafka-processor-cosmos-block/actions/workflows/test.yml)
[![conventional commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg?style=for-the-badge&logo=conventionalcommits)](https://conventionalcommits.org)
[![contributor covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg?style=for-the-badge)](https://github.com/okp4/.github/blob/main/CODE_OF_CONDUCT.md)
[![License](https://img.shields.io/badge/License-BSD_3--Clause-blue.svg?style=for-the-badge)](https://opensource.org/licenses/BSD-3-Clause)
[![Quarkus](https://img.shields.io/badge/Quarkus-1A2C34?logo=quarkus&logoColor=4695EB&style=for-the-badge)](https://quarkus.io)

## Purpose

The Kafka Processor `CØSMOS-Block` is basically a Stream Processor which continuously
reads [CØSMOS blocks](https://docs.cosmos.network/master/intro/sdk-app-architecture.html) from an `input` Kafka topic,
unwraps the CØSMOS transactions and sends them to an `output` topic.

<p align="center">
  <img src="./docs/overview.png">
</p>

This processor plays well with the [CØSMOS Kafka Connector](https://github.com/okp4/kafka-connector-cosmos)
which provides Kafka with CØSMOS blocks ready to be processed.

## Implementation

Implementation mainly relies on [Kafka Streams API](https://kafka.apache.org/documentation/streams), library to create
event-stream applications with the following features:

- no external dependency other than Kafka itself,
- simple and light library,
- fault-tolerant and scalable.

Moreover, this implementation:

- uses [Kotkin](https://kotlinlang.org/) as primary coding language,
- uses [Quarkus](https://quarkus.io/) to minimize resources consumption,
- is as much as possible, lean, i.e. tries to minimize the dependencies to 3rd party libraries and the resulting package
  footprint.

## Build

This project targets the [JVM 11+](https://openjdk.java.net/), so be sure to have it available in your environment.

This project relies on the [Gradle](https://gradle.org/) build system.

If you are on Windows then open a command line, go into the root directory and run:

```sh
.\gradlew build
```

If you are on linux/mac then open a terminal, go into the root directory and run:

```sh
./gradlew build
```

This command line produces a _native_ executable: `kafka-processor-cosmos-block-X.Y-runner`

## You want to get involved? 😍

Please check out OKP4 health files :

- [Contributing](https://github.com/okp4/.github/blob/main/CONTRIBUTING.md)
- [Code of conduct](https://github.com/okp4/.github/blob/main/CODE_OF_CONDUCT.md)
