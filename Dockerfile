FROM eclipse-temurin:17-jre-alpine AS base

# Install required tools
RUN apk add --no-cache curl wget ca-certificates bash

WORKDIR /app

# BSL Language Server setup
ARG BSL_VERSION=latest
ARG GITHUB_TOKEN
ARG BSL_JAR_PATH=/opt/bsl/bsl-language-server.jar

RUN mkdir -p /opt/bsl && \
    echo "Downloading BSL Language Server version: $BSL_VERSION" && \
    if [ "$BSL_VERSION" = "latest" ]; then \
        if [ -n "$GITHUB_TOKEN" ]; then \
            echo "Using GitHub token for latest release (no rate limits)" && \
            DOWNLOAD_URL=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
                https://api.github.com/repos/1c-syntax/bsl-language-server/releases/latest | \
                grep "browser_download_url.*-exec\.jar" | \
                head -n 1 | \
                cut -d '"' -f 4); \
        else \
            echo "No GitHub token provided (may hit rate limits)" && \
            DOWNLOAD_URL=$(curl -s \
                https://api.github.com/repos/1c-syntax/bsl-language-server/releases/latest | \
                grep "browser_download_url.*-exec\.jar" | \
                head -n 1 | \
                cut -d '"' -f 4); \
        fi; \
    else \
        echo "Using specific version: $BSL_VERSION" && \
        DOWNLOAD_URL="https://github.com/1c-syntax/bsl-language-server/releases/download/${BSL_VERSION}/bsl-language-server-${BSL_VERSION}-exec.jar"; \
    fi && \
    echo "Downloading from: $DOWNLOAD_URL" && \
    wget -O "$BSL_JAR_PATH" "$DOWNLOAD_URL" && \
    chmod +x "$BSL_JAR_PATH"

# Copy application JAR
COPY build/libs/*.jar app.jar

# Volumes
VOLUME ["/workspaces"]

# Environment variables
ENV WEB_UI_PORT=9090
ENV MCP_TRANSPORT=stdio
ENV MCP_PORT=8080
ENV LOGGING_ENABLED=true
ENV BSL_JAR_PATH=/opt/bsl/bsl-language-server.jar
ENV BSL_MAX_HEAP=4g

# Expose ports
EXPOSE 9090
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

