
FROM gradle:8.7-jdk17 AS build
WORKDIR /src
COPY . .
RUN gradle --no-daemon clean shadowJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/build/libs/*-all.jar /app/app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
