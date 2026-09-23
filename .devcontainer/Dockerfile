FROM eclipse-temurin:25-jdk-alpine

# links the GHCR package to this repository and documents the image
LABEL org.opencontainers.image.source="https://github.com/Crisissupercool/TicTacTestAssignment"
LABEL org.opencontainers.image.title="TicTacToe DevContainer"
LABEL org.opencontainers.image.description="Java 25 + Gradle development environment for the M450 TicTacToe project"

ARG GRADLE_VERSION=9.7.0

RUN apk add --no-cache bash git unzip wget python3 \
    && wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle \
    && rm /tmp/gradle.zip \
    && addgroup -g 1000 vscode \
    && adduser -D -u 1000 -G vscode vscode

USER vscode
