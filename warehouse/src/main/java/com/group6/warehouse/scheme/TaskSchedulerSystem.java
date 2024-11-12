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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class TaskSchedulerSystem {

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
    // Chạy vào lúc 0:00 các ngày Thứ Hai, Thứ Tư, Thứ Sáu
    @Scheduled(cron = "0 00 00 * * 1,3,5")
    public void TaskScheduler() {
        executeTask();
    }

    public void executeTask() {
        System.out.println("Scheduled task executed at: " + LocalDateTime.now());
        try {
            runExternalExecutable();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public void runExternalExecutable() throws IOException, MessagingException {
        DataFileConfig dataFileConfig = null;
        try {
            dataFileConfig = dataFileConfigRepository.findById(1L).orElse(null);
        } catch (Exception e) {
            Log log = Log.builder()
                    .idConfig(1L)
                    .taskName("Get data config")
                    .status("failure")
                    .message("Connect to database control failed! Check your database or network")
                    .level(LevelEnum.ERROR)
                    .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                    .build();
            sendMail.sendEmail(emailA, log);
            e.printStackTrace();
            return;
        }
        LocalDate today = LocalDate.now();
        String date = String.format("%d_%d_%d", today.getDayOfMonth(), today.getMonthValue(), today.getYear());
        String pathDestination = dataFileConfig.getDirectoryFile();
        String fileDestination = dataFileConfig.getFilename() + date + "." + dataFileConfig.getFormat().toUpperCase();

        ProcessBuilder processBuilder = new ProcessBuilder("python", pathScript, pathDestination, fileDestination);
        processBuilder.redirectErrorStream(true);
        System.out.println("Loading...");
        Log log = Log.builder()
                .idConfig(1L)
                .taskName("Crawl data")
                .status("Processing")
                .message("Crawl data from Tiki.vn")
                .level(LevelEnum.INFO)
                .build();
        logRepository.save(log);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int exitCode;
        try {
            exitCode = process.waitFor();
            System.out.println("Python script executed with exit code: " + exitCode);
            if (exitCode == 0) {
                log.setStatus("Successful");
                log.setEndTime(Timestamp.valueOf(LocalDateTime.now()));
                logRepository.save(log);
                sendMail.sendEmail(emailA, log);
                System.out.println("Script executed successfully!");
            } else {
                log.setStatus("Failure");
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
            log.setStatus("Failure");
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

    public static void main(String[] args) {
        System.out.println(LocalDateTime.now());
    }
}
