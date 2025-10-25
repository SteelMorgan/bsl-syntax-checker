#!/bin/bash

# Simple stdio mode runner that disables web server
export MCP_TRANSPORT=stdio
export LOGGING_ENABLED=false
export SERVER_PORT=-1
export SPRING_MAIN_WEB_APPLICATION_TYPE=none

# Run the application
java -jar /app/app.jar