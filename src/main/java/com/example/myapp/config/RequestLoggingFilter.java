package com.example.myapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Journalise chaque appel HTTP et attribue un identifiant de corrélation
 * (traceId) placé dans le MDC : toutes les lignes de log produites pendant la
 * requête (y compris celles de {@link LoggingAspect}) partagent ce même id,
 * ce qui permet de suivre un appel de bout en bout.
 *
 * Ligne d'accès (méthode, chemin + query, statut, durée) au niveau INFO ;
 * ligne d'arrivée avec l'IP source au niveau DEBUG.
 * Niveau réglable via : logging.level.http.access
 * Les appels /actuator/** sont ignorés (bruit des health checks).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");
    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Réutilise un traceId fourni par le proxy s'il existe, sinon en génère un.
        String traceId = request.getHeader("X-Request-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader("X-Request-Id", traceId);

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
            MDC.remove(TRACE_ID);
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
