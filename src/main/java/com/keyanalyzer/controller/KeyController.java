package com.keyanalyzer.controller;

import com.keyanalyzer.model.KeyRequest;
import com.keyanalyzer.model.KeyResponse;
import com.keyanalyzer.service.KeyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api")
public class KeyController {

    private static final long TIMEOUT_SECONDS = 10;

    private final KeyService keyService;
    private final ExecutorService requestTimeoutExecutor;

    public KeyController(KeyService keyService, ExecutorService requestTimeoutExecutor) {
        this.keyService = keyService;
        this.requestTimeoutExecutor = requestTimeoutExecutor;
    }

    @PostMapping("/compute-keys")
    public ResponseEntity<?> computeKeys(@Valid @RequestBody KeyRequest request) {
        CompletableFuture<KeyResponse> future = CompletableFuture.supplyAsync(
                () -> keyService.computeKeys(request),
                requestTimeoutExecutor
        );

        try {
            KeyResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return ResponseEntity.ok(response);
        } catch (TimeoutException e) {
            future.cancel(true);
            return ResponseEntity.status(408).body(
                    Map.of("error", "Computation timed out after " + TIMEOUT_SECONDS + " seconds. Try reducing the number of attributes.")
            );
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                return ResponseEntity.badRequest().body(Map.of("error", cause.getMessage()));
            }
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Internal error: " + cause.getMessage())
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Computation was interrupted.")
            );
        }
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
