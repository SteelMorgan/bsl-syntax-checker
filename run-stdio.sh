#!/bin/bash
# MCP BSL Server - stdio mode launcher

export TRANSPORT_MODE=stdio
export LOGGING_ENABLED=false

java -jar build/libs/mcp-bsl-server-0.1.0-SNAPSHOT.jar

