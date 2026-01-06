# syntax=docker/dockerfile:1.6
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.logging,java.sql,java.naming,java.management,java.desktop,java.instrument,jdk.unsupported,java.compiler \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /opt/java-runtime

FROM gcr.io/distroless/cc-debian12:nonroot

WORKDIR /app
ENV JAVA_HOME=/opt/java-runtime
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=builder /opt/java-runtime /opt/java-runtime
COPY --from=builder /workspace/target/im-0.0.1-SNAPSHOT.jar /app/im.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/im.jar"]
