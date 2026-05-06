FROM mcr.microsoft.com/devcontainers/java:3.0.7-25-jdk-trixie

ARG GRADLE_VERSION=9.5.0
ARG KOTLIN_VERSION=2.3.21

RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/* \
    && install -d /usr/local/sdkman/candidates/java \
    && ln -sfn /usr/lib/jvm/msopenjdk-current /usr/local/sdkman/candidates/java/current \
    && curl -fsSL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -sf "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle \
    && curl -fsSL -o /tmp/kotlin-compiler.zip "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip" \
    && unzip /tmp/kotlin-compiler.zip -d /usr/local \
    && ln -sf /usr/local/kotlinc/bin/kotlin /usr/local/bin/kotlin \
    && ln -sf /usr/local/kotlinc/bin/kotlinc /usr/local/bin/kotlinc \
    && rm -f /tmp/gradle.zip /tmp/kotlin-compiler.zip
