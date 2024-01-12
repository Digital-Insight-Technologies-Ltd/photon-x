FROM public.ecr.aws/docker/library/maven:3.9.6-amazoncorretto-21-debian-bookworm AS build-image

WORKDIR /app
COPY src ./src
COPY pom.xml .

RUN mvn package -Dmaven.test.skip=true

FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023-arm64 AS runtime-image

COPY --from=build-image /app/target/photon-*.jar /app/photon.jar

EXPOSE 2322

ENTRYPOINT ["java", "-jar", "/app/photon.jar"]