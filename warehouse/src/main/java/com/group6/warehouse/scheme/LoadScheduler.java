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
    // Đường dẫn tới file JAR của bạn
    @Value("${custom.file.loadStagingPath}")
    private String SCRIPT_PATH ;
    private static final int MAX_RETRY_DURATION = 60; // Thời gian retry tối đa là 60 phút
    private static final int RETRY_INTERVAL = 10; // Thử lại mỗi 10 phút
    /**
     * Lập lịch chạy script vào lúc 00:30 sáng thứ 2, thứ 4, thứ 6 hàng tuần.
     * Cron expression: "0 30 0 * * MON,WED,FRI"
     */
    @Scheduled(cron = "00 37 11 * * MON,WED,FRI,SAT")
    public void runLoadToStagingScript() {
        boolean success = false;
        int retryCount = 0;
        // Lặp lại chạy script trong 1 giờ, thử lại mỗi 10 phút nếu chưa thành công
        while (retryCount < MAX_RETRY_DURATION && !success) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", SCRIPT_PATH);
                // Thực thi script
                Process process = processBuilder.start();

                // Đọc đầu ra của tiến trình
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    // In ra màn hình log của tiến trình
                    System.out.println(line);
                }

                int exitCode = process.waitFor(); // Chờ kết quả của process
                if (exitCode == 0) {
                    // Nếu script chạy thành công
                    System.out.println("LoadToStaging.jar executed successfully at: " + LocalDateTime.now());
                    success = true;
                } else {
                    // Nếu script thất bại
                    System.err.println("LoadToStaging.jar execution failed at: " + LocalDateTime.now() + ". Retrying in 10 minutes...");
                    retryCount++;
                    if (retryCount < MAX_RETRY_DURATION) {
                        // Đợi 10 phút trước khi thử lại
                        Thread.sleep(RETRY_INTERVAL * 60 * 1000); // Thời gian nghỉ giữa các lần thử lại
                    }
                }
            } catch (IOException | InterruptedException e) {
                // Xử lý ngoại lệ nếu có
                System.err.println("Error while executing LoadToStaging.jar: " + e.getMessage());
                e.printStackTrace();
                break; // Nếu có lỗi nghiêm trọng, dừng lại
            }
        }
        if (!success) {
            // Nếu sau 1 giờ mà chưa thành công
            System.err.println("LoadToStaging.jar failed after 1 hour of retries.");
        }
    }
}
