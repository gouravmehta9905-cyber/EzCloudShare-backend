# Stage 1: Build
FROM maven:3.9.5-eclipse-temurin-21 AS build

WORKDIR /EzCloudShare
COPY . .

RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jdk

WORKDIR /EzCloudShare

COPY --from=build /EzCloudShare/target/*.jar EzCloudShare.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "EzCloudShare.jar"]