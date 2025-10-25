package com.github.steel33ff.mcpbsl.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Instant

/**
 * Interceptor for logging HTTP requests
 */
@Component
class LoggingInterceptor : HandlerInterceptor {
    private val logger = KotlinLogging.logger {}

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val startTime = System.currentTimeMillis()
        request.setAttribute("startTime", startTime)
        
        logger.info { 
            "HTTP Request: ${request.method} ${request.requestURI} " +
            "from ${request.remoteAddr} " +
            "User-Agent: ${request.getHeader("User-Agent") ?: "unknown"}"
        }
        
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest, 
        response: HttpServletResponse, 
        handler: Any, 
        ex: Exception?
    ) {
        val startTime = request.getAttribute("startTime") as? Long ?: System.currentTimeMillis()
        val duration = System.currentTimeMillis() - startTime
        
        logger.info { 
            "HTTP Response: ${request.method} ${request.requestURI} " +
            "Status: ${response.status} " +
            "Duration: ${duration}ms " +
            "from ${request.remoteAddr}"
        }
        
        if (ex != null) {
            logger.error(ex) { 
                "HTTP Error: ${request.method} ${request.requestURI} " +
                "Status: ${response.status} " +
                "Duration: ${duration}ms"
            }
        }
    }
}
