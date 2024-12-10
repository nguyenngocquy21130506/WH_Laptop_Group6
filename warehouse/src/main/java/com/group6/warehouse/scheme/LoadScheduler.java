package com.group6.warehouse.scheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
@Component
public class LoadScheduler {
    @Value("${custom.file.loadStagingPath}")
    private  String SCRIPT_PATH ;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_INTERVAL = 1;

    @Scheduled(cron = "00 20 00 * * MON,WED,FRI")
    public void scheduleLoadToStagingScript() {
        runLoadToStagingScript();
    }

    public void runLoadToStagingScript() {
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            if (executeJarScript()) {
                System.out.println("Script executed successfully. No further retries.");
                return;
            }

            retryCount++;
            if (retryCount < MAX_RETRY_COUNT) {
                System.err.println("Retrying in " + RETRY_INTERVAL + " minute(s)...");
                try {
                    Thread.sleep(RETRY_INTERVAL * 60 * 1000);
                } catch (InterruptedException e) {
                    System.err.println("Retry sleep interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        System.err.println("LoadToStaging.jar failed after " + MAX_RETRY_COUNT + " retries.");
    }

    private boolean executeJarScript() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", SCRIPT_PATH);
            Process process = processBuilder.start();

            boolean success = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (line.contains("Success") || line.contains("completed")) {
                        success = true;
                        break;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && success) {
                System.out.println("LoadToStaging.jar executed successfully at: " + LocalDateTime.now());
                return true;
            } else {
                System.err.println("LoadToStaging.jar execution failed at: " + LocalDateTime.now());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error while executing LoadToStaging.jar: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}