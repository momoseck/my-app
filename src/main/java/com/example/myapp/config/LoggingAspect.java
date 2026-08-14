package com.example.myapp.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Trace les appels de méthodes des couches "controller" et "service".
 *
 * Pour chaque méthode :
 *   - INFO  : ligne d'entrée ">>" et ligne de sortie "<<" avec la durée
 *   - DEBUG : ajoute les arguments (entrée) et la valeur de retour (sortie)
 *   - ERROR : "!!" si la méthode lève une exception
 *
 * Le logger porte le nom de la classe appelée, et chaque ligne est préfixée par
 * le traceId de la requête (voir RequestLoggingFilter + logging.pattern.console),
 * ce qui permet de suivre un appel de bout en bout.
 *
 * Verbosité réglable via : logging.level.com.example.myapp
 *   INFO (défaut) = flux des méthodes ; DEBUG = args + retours ; WARN = silence.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final int MAX_VALUE_LENGTH = 300;

    @Pointcut("within(com.example.myapp.controller..*)")
    public void controllerLayer() {
    }

    @Pointcut("within(com.example.myapp.service..*)")
    public void serviceLayer() {
    }

    @Around("controllerLayer() || serviceLayer()")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());
        String method = joinPoint.getSignature().getName();

        if (log.isDebugEnabled()) {
            log.debug(">> {}({})", method, formatArgs(joinPoint.getArgs()));
        } else if (log.isInfoEnabled()) {
            log.info(">> {}()", method);
        }

        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long ms = elapsedMs(start);
            if (log.isDebugEnabled()) {
                log.debug("<< {}() [{} ms] => {}", method, ms, summarize(result));
            } else if (log.isInfoEnabled()) {
                log.info("<< {}() [{} ms]", method, ms);
            }
            return result;
        } catch (Throwable ex) {
            long ms = elapsedMs(start);
            log.error("!! {}() [{} ms] a levé {}: {}", method, ms,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        return summarize(Arrays.toString(args));
    }

    private String summarize(Object value) {
        String s = String.valueOf(value);
        if (s.length() > MAX_VALUE_LENGTH) {
            return s.substring(0, MAX_VALUE_LENGTH) + "…(tronqué)";
        }
        return s;
    }
}
