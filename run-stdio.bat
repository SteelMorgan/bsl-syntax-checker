@echo off
REM MCP BSL Server - stdio mode launcher (Windows)

set TRANSPORT_MODE=stdio
set LOGGING_ENABLED=false

java -jar build\libs\mcp-bsl-server-0.1.0-SNAPSHOT.jar

