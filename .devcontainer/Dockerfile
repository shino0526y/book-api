FROM mcr.microsoft.com/devcontainers/base:debian

ARG GRADLE_VERSION=9.5.0
ARG KOTLIN_VERSION=2.3.21
ARG JAVA_VERSION=25
ARG TARGETARCH

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip postgresql postgresql-client \
    && rm -rf /var/lib/apt/lists/* \
    && install -d /usr/local/java /usr/local/sdkman/candidates/java \
    && case "${TARGETARCH}" in \
        amd64) java_arch='x64' ;; \
        arm64) java_arch='aarch64' ;; \
        *) echo "Unsupported TARGETARCH: ${TARGETARCH}" >&2; exit 1 ;; \
    esac \
    && curl -fsSL -o /tmp/java.tar.gz "https://api.adoptium.net/v3/binary/latest/${JAVA_VERSION}/ga/linux/${java_arch}/jdk/hotspot/normal/eclipse?project=jdk" \
    && tar -xzf /tmp/java.tar.gz -C /usr/local/java \
    && ln -sfn /usr/local/java/$(find /usr/local/java -mindepth 1 -maxdepth 1 -type d | xargs -n1 basename | head -n1) /usr/local/java/current \
    && ln -sfn /usr/local/java/current /usr/local/sdkman/candidates/java/current \
    && ln -sf /usr/local/java/current/bin/java /usr/local/bin/java \
    && ln -sf /usr/local/java/current/bin/javac /usr/local/bin/javac \
    && curl -fsSL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -sf "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle \
    && curl -fsSL -o /tmp/kotlin-compiler.zip "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip" \
    && unzip -q /tmp/kotlin-compiler.zip -d /usr/local \
    && ln -sf /usr/local/kotlinc/bin/kotlin /usr/local/bin/kotlin \
    && ln -sf /usr/local/kotlinc/bin/kotlinc /usr/local/bin/kotlinc \
    && rm -f /tmp/java.tar.gz /tmp/gradle.zip /tmp/kotlin-compiler.zip

ENV JAVA_HOME=/usr/local/java/current
ENV PATH=${JAVA_HOME}/bin:${PATH}

RUN echo 'vscode ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/99-vscode \
    && chmod 0440 /etc/sudoers.d/99-vscode

RUN mkdir -p /var/run/postgresql && chown postgres:postgres /var/run/postgresql

EXPOSE 5432
