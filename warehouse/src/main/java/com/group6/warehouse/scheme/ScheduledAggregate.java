package com.group6.warehouse.scheme;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Component
public class ScheduledAggregate {

    @Value("${custom.file.aggregatePath}")
    private String aggregatePath;

    private static final int MAX_RETRIES = 5;  // Số lần thử tối đa
    private static final int RETRY_DELAY_MS = 60000;  // Thời gian delay giữa các lần thử (1 phút = 60000 ms)
    private int retryCount = 0;  // Biến đếm số lần thử lại

    // Lập lịch
//    @Scheduled(cron = "0 35 0 * * 2,4,6")
    @Scheduled(cron = "0 25 14 * * ?")
    public void executeAggregateJar() {
        try {
            System.out.println("Scheduled task started at: " + LocalDateTime.now());  // In ra thời điểm bắt đầu
            // Gọi phương thức thực thi tiến trình
            runExternalExecutable();
            System.out.println("Scheduled task completed successfully at: " + LocalDateTime.now());  // In ra thời điểm kết thúc thành công
        } catch (IOException | InterruptedException e) {
            // Nếu gặp lỗi, gọi phương thức retryTask để thử lại
            retryTask(e);
        }
    }

    // Phương thức thử lại tiến trình nếu thất bại
    public void retryTask(Exception initialException) {
        while (retryCount < MAX_RETRIES) {
            try {
                retryCount++;  // Tăng số lần thử
                System.out.println("Retry attempt " + retryCount + " started at: " + LocalDateTime.now());  // In ra thời điểm thử lại

                // Thực hiện lại tiến trình
                runExternalExecutable();  // Gọi lại phương thức thực thi tiến trình
                System.out.println("Task executed successfully on attempt " + retryCount + " at: " + LocalDateTime.now());  // In ra kết quả thành công
                break;  // Nếu thành công thì thoát khỏi vòng lặp

            } catch (IOException | InterruptedException e) {
                // Bắt tất cả các ngoại lệ liên quan đến I/O, SQL, hoặc InterruptedException
                e.printStackTrace();
                System.err.println("An error occurred during attempt " + retryCount + ": " + e.getMessage() + " at: " + LocalDateTime.now());

                // Nếu đã thử đủ 5 lần, dừng lại
                if (retryCount == MAX_RETRIES) {
                    System.err.println("Maximum retry attempts reached. Stopping at: " + LocalDateTime.now());
                    break;  // Dừng lại sau khi đã thử đủ lần
                }
            }

            // Nếu chưa đạt số lần thử tối đa, delay 1 phút trước khi thử lại
            if (retryCount < MAX_RETRIES) {
                try {
                    System.out.println("Waiting for 1 minute before retrying... at: " + LocalDateTime.now());
                    Thread.sleep(RETRY_DELAY_MS);  // Thời gian delay 1 phút
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();  // Khôi phục trạng thái interrupt
                }
            }
        }
    }

    // Phương thức chạy file .jar
    public void runExternalExecutable() throws IOException, InterruptedException {
        System.out.println("Starting the execution of .jar file at: " + LocalDateTime.now());
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", aggregatePath);
        processBuilder.directory(new File("."));  // Thư mục làm việc hiện tại
        Process process = processBuilder.start();
        int exitCode = process.waitFor();  // Chờ tiến trình hoàn thành
        if (exitCode != 0) {
            System.err.println("Error executing jar file at: " + LocalDateTime.now() + ", exit code: " + exitCode);
            throw new IOException("Error executing jar file, exit code: " + exitCode);  // Ném ngoại lệ nếu tiến trình không thành công
        }
        System.out.println("Jar file executed successfully at: " + LocalDateTime.now());
    }
}
