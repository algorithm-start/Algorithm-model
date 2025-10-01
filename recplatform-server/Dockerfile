# Multi-stage build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Configure Aliyun Maven mirror for faster downloads
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>\
<settings><mirrors><mirror>\
<id>aliyun</id>\
<mirrorOf>central</mirrorOf>\
<name>Aliyun Maven Mirror</name>\
<url>https://maven.aliyun.com/repository/public</url>\
</mirror></mirrors></settings>' > /root/.m2/settings.xml

COPY pom.xml .
COPY recplatform-common/pom.xml recplatform-common/
COPY recplatform-solver/pom.xml recplatform-solver/
COPY recplatform-data/pom.xml recplatform-data/
COPY recplatform-orchestrator/pom.xml recplatform-orchestrator/
COPY recplatform-iam/pom.xml recplatform-iam/
COPY recplatform-audit/pom.xml recplatform-audit/
COPY recplatform-web/pom.xml recplatform-web/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
# Set timezone to China Standard Time so that LocalDateTime.now() and all logs
# use Asia/Shanghai instead of UTC. The Debian-based image already ships tzdata.
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone
ENV TZ=Asia/Shanghai
COPY --from=builder /app/recplatform-web/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Duser.timezone=Asia/Shanghai"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
