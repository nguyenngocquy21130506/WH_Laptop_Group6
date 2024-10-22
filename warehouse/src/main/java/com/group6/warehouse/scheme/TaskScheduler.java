package com.group6.warehouse.scheme;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class TaskScheduler {

    // Sử dụng cron expression để lên lịch chạy sau mỗi 3 ngày
    @Scheduled(cron = "0 0 0 */3 * *") // Chạy mỗi 3 ngày lúc 00:00
    public void executeTask() {
        try {
            // Thực thi một file hoặc tiến trình
            runExternalExecutable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Phương thức để thực thi file .exe
    public void runExternalExecutable() throws IOException {
        // Đường dẫn đến file .exe (thay đổi theo nhu cầu)
        String exeFilePath = "path/to/your/executable.exe";

        ProcessBuilder processBuilder = new ProcessBuilder(exeFilePath);

        // Thiết lập nếu bạn muốn nhận đầu ra
        processBuilder.redirectErrorStream(true); // Gộp luồng lỗi vào luồng đầu ra

        // Chạy tiến trình
        Process process = processBuilder.start();

        // Kiểm tra đầu ra của tiến trình (nếu cần)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Chờ đến khi tiến trình kết thúc
        int exitCode;
        try {
            exitCode = process.waitFor();
            System.out.println("Executable executed with exit code: " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
