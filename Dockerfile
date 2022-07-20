# Stage 1 : build
FROM quay.io/quarkus/ubi-quarkus-native-s2i:22.1-java11 AS build

COPY --chown=quarkus:quarkus . /usr/src/app

WORKDIR /usr/src/app

RUN ./gradlew build -x check -Dquarkus.package.type=native --no-daemon

# Stage 2 : run
FROM quay.io/quarkus/quarkus-micro-image:1.0

WORKDIR /work/

COPY --from=build /usr/src/app/build/*-runner /work/application

EXPOSE 8080

ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]
