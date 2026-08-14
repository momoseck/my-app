package com.example.myapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Journalise chaque appel HTTP : méthode, chemin (+ query string), code de
 * réponse et durée. Les appels vers /actuator/** sont ignorés pour éviter le
 * bruit des health checks.
 *
 * Niveau contrôlable via la propriété : logging.level.http.access
 * (INFO par défaut ; passe à DEBUG/TRACE ou OFF pour ajuster).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String query = request.getQueryString();
        String path = request.getRequestURI() + (query != null ? "?" + query : "");

        if (log.isDebugEnabled()) {
            log.debug("--> {} {} (from {})", request.getMethod(), path, remoteAddress(request));
        }

        int status = 0;
        try {
            filterChain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("{} {} -> {} ({} ms)", request.getMethod(), path, status, duration);
        }
    }

    /** Respecte l'en-tête X-Forwarded-For (présent derrière Nginx / le proxy Coolify). */
    private String remoteAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
