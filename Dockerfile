FROM maven:3.6.0-jdk-8 as builder

COPY alpha /servicecomb-pack/alpha
COPY pack-common /servicecomb-pack/pack-common
COPY pom.xml /servicecomb-pack/pom.xml

WORKDIR /servicecomb-pack/alpha

RUN mvn clean package --batch-mode -DskipTests=true

FROM openjdk:8-jre-alpine

LABEL Description="This is an experimental image for the master branch of the ServiceComb Pack" Vendor="ServiceComb Project"

COPY --from=builder /servicecomb-pack/alpha/alpha-server/target/saga/alpha-server-0.5.0-SNAPSHOT-exec.jar /alpha-server-0.5.0-SNAPSHOT-exec.jar

CMD ["java -jar alpha-server-0.5.0-SNAPSHOT-exec.jar"]