package com.keyanalyzer.controller;

import com.keyanalyzer.model.KeyRequest;
import com.keyanalyzer.model.KeyResponse;
import com.keyanalyzer.service.KeyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api")
public class KeyController {

    private static final long TIMEOUT_SECONDS = 10;

    private final KeyService keyService;

    public KeyController(KeyService keyService) {
        this.keyService = keyService;
    }

    @PostMapping("/compute-keys")
    public ResponseEntity<?> computeKeys(@Valid @RequestBody KeyRequest request) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<KeyResponse> future = executor.submit(() -> keyService.computeKeys(request));

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
        } finally {
            executor.shutdownNow();
        }
    }
}
