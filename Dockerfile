FROM public.ecr.aws/docker/library/maven:3.9.6-amazoncorretto-21-debian-bookworm AS build-image

WORKDIR /app
COPY src ./src
COPY pom.xml .

RUN mvn clean package
RUN apt update -y && apt upgrade -y && apt install -y curl
RUN curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar


FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023-arm64 AS runtime-image

COPY --from=build-image /app/target/photon-*.jar /app/photon.jar
COPY --from=build-image /app/opentelemetry-javaagent.jar /app/otel.jar

ENV JAVA_TOOL_OPTIONS = "-javaagent:/app/otel.jar"

EXPOSE 2322

ENTRYPOINT ["java", "-jar", "/app/photon.jar"]