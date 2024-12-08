package com.group6.warehouse.scheme;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class MartScheduled {

    @Value("${custom.file.loadMartPath}")
    private String loadMartPath;

    private static final int MAX_RETRIES = 5;  // Số lần thử tối đa
    private static final int RETRY_DELAY_MS = 60000;  // Thời gian delay giữa các lần thử (1 phút = 60000 ms)
    private int retryCount = 0;  // Biến đếm số lần thử lại
    @Autowired
    private JdbcTemplate jdbcTemplate;


    // Lập lịch
    @Scheduled(cron = "30 30 13 * * ?")
    public void executeLoadMartJar() {
        try {
            // Gọi phương thức thực thi file .jar
            runExternalExecutable();

         
            if (isFailLogDetected()) {
                System.out.println("Fail log detected for LoadDataToMart. Retrying...");
                retryTask(new IOException("Fail log detected for LoadDataToMart"));
            } else {
                System.out.println("Scheduled task completed successfully at: " + LocalDateTime.now());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Kiểm tra xem có "Fail" trong log của task_name 'LoadDataToMart' không
    private boolean isFailLogDetected() {
        String sql = "SELECT status FROM logs WHERE task_name = 'LoadDataToMart' ORDER BY id DESC LIMIT 1";

        // Lấy trạng thái log từ cơ sở dữ liệu (giả sử có trường status và task_name trong bảng logs)
        String status = jdbcTemplate.queryForObject(sql, String.class);

        return "Fail".equalsIgnoreCase(status);  // Kiểm tra nếu trạng thái là "Fail"
    }

    // Phương thức thử lại tiến trình nếu trạng thái log là "Fail"
    public void retryTask(Exception initialException) {
        while (retryCount < MAX_RETRIES) {
            try {
                retryCount++;
                System.out.println("Retry attempt " + retryCount + " started at: " + LocalDateTime.now());

                // Kiểm tra lại trạng thái log trước khi quyết định retry
                if (isFailLogDetected()) {
                    System.out.println("Fail log detected for LoadDataToMart again. Retrying...");
                    runExternalExecutable();  // Chạy lại tiến trình
                } else {
                    // Nếu lần retry thành công, dừng vòng lặp và không retry nữa
                    System.out.println("Task executed successfully on attempt " + retryCount + " at: " + LocalDateTime.now());
                    break;  // Dừng lại nếu log không phải "Fail"
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                System.err.println("Error during attempt " + retryCount + ": " + e.getMessage());

                if (retryCount == MAX_RETRIES) {
                    System.err.println("Maximum retry attempts reached. Stopping at: " + LocalDateTime.now());
                    break;  // Dừng retry nếu đã đạt số lần thử tối đa
                }
            }

            // Nếu chưa đạt số lần thử tối đa, delay 1 phút trước khi thử lại
            if (retryCount < MAX_RETRIES) {
                try {
                    System.out.println("Waiting for 1 minute before retrying... at: " + LocalDateTime.now());
                    Thread.sleep(RETRY_DELAY_MS);  // Thời gian delay 1 phút
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Phương thức chạy file .jar
    public void runExternalExecutable() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", loadMartPath);
        processBuilder.directory(new File("."));  // Thư mục làm việc hiện tại
        Process process = processBuilder.start();
        int exitCode = process.waitFor();  // Chờ tiến trình hoàn thành

        // Kiểm tra trạng thái exit của tiến trình
        if (exitCode != 0) {
            System.err.println("Error executing jar file at: " + LocalDateTime.now() + ", exit code: " + exitCode);
            throw new IOException("Error executing jar file, exit code: " + exitCode);  // Ném ngoại lệ nếu tiến trình không thành công
        }

    }
}
