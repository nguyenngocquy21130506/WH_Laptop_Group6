package com.group6.warehouse.scheme;

import com.group6.warehouse.control.model.DataFileConfig;
import com.group6.warehouse.control.model.LevelEnum;
import com.group6.warehouse.control.model.Log;
import com.group6.warehouse.control.repository.DataFileConfigRepository;
import com.group6.warehouse.control.repository.LogRepository;
import com.group6.warehouse.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class TaskScheduler {

    @Value("${email.user.hostA}")
    String emailA;

    @Autowired
    LogRepository logRepository;
    @Autowired
    DataFileConfigRepository dataFileConfigRepository;

    @Autowired
    SendMail sendMail;

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
        DataFileConfig dataFileConfig = dataFileConfigRepository.findById(1L).orElse(null);
        LocalDate today = LocalDate.now();
        String date = String.format("%d_%d_%d", today.getDayOfMonth(), today.getMonthValue(), today.getYear());
        String pathDestination = dataFileConfig.getDirectoryFile();
        String fileDestination = dataFileConfig.getFilename() + date + "." + dataFileConfig.getFormat().toUpperCase();

        // Tạo ProcessBuilder để chạy script Python
        ProcessBuilder processBuilder = new ProcessBuilder("python", pathScript, pathDestination, fileDestination);
        processBuilder.redirectErrorStream(true); // Gộp luồng lỗi vào luồng đầu ra

        // Chạy tiến trình
        Process process = processBuilder.start();
        Log log = Log.builder()
                .idConfig(1L)
                .taskName("Crawl data")
                .status("Process")
                .message("Crawl data from Tiki.vn")
                .level(LevelEnum.INFO)
                .build();
        logRepository.save(log);
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
                log.setStatus("Success");
                log.setEndTime(LocalDateTime.now());
                logRepository.save(log);
                System.out.println("Script executed successfully!");
            } else {
                log.setStatus("Failed");
                log.setEndTime(LocalDateTime.now());
                log.setLevel(LevelEnum.ERROR);
                logRepository.save(log);
                System.out.println("Script execution failed with exit code: " + exitCode);
                try {
                    sendMail.sendEmail(emailA, log);
                } catch (MessagingException e) {
                    System.out.println("Send mail error! Check your email or network");
                    throw new RuntimeException(e);
                }
            }
        } catch (InterruptedException e) {
            log.setStatus("Failed");
            log.setEndTime(LocalDateTime.now());
            log.setLevel(LevelEnum.ERROR);
            logRepository.save(log);
            try {
                sendMail.sendEmail(emailA, log);
            } catch (MessagingException ex) {
                System.out.println("Send mail error! Check your email or network");
                throw new RuntimeException(ex);
            }
            e.printStackTrace();
        }
    }
}
