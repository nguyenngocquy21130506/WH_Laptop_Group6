package com.group6.warehouse.scheme;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.time.LocalDate;

@Component
public class TaskScheduler {
    @Value("${custom.file.crawlPath}")
    private String pathScript;

    @PostConstruct
    public void TaskScheduler() {
        executeTask();
    }

    public void executeTask() {
        try {
            runExternalExecutable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runExternalExecutable() throws IOException {
        LocalDate today = LocalDate.now();
        String date = String.format("%d_%d_%d", today.getDayOfMonth(), today.getMonthValue(), today.getYear());
        String fileDestination = "dataFeed_tiki_" + date; // tên file đầu ra

        // Tạo ProcessBuilder để chạy script Python
        ProcessBuilder processBuilder = new ProcessBuilder("python", pathScript, fileDestination);
        processBuilder.redirectErrorStream(true); // Gộp luồng lỗi vào luồng đầu ra

        // Chạy tiến trình
        Process process = processBuilder.start();

        // Kiểm tra đầu ra của tiến trình
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // In ra từng dòng kết quả
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Chờ đến khi tiến trình kết thúc và thông báo
        int exitCode;
        try {
            exitCode = process.waitFor();
            System.out.println("Python script executed with exit code: " + exitCode); // Thông báo sau khi hoàn thành
            if (exitCode == 0) {
                System.out.println("Script executed successfully!");
            } else {
                System.out.println("Script execution failed with exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
