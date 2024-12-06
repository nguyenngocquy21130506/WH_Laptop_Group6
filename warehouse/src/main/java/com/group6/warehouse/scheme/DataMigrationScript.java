package com.group6.warehouse.scheme;

import com.group6.warehouse.control.model.DataFileConfig;
import com.group6.warehouse.control.model.LevelEnum;
import com.group6.warehouse.control.model.Log;
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
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataMigrationScript {

    @Value("${custom.file.statowhPath}")
    private String pathScript;

    @Value("${retry.attempts}")
    private int retryCount;

    @Value("${retry.delay}")
    private int delay;

    // Chạy vào lúc 0:00 các ngày Thứ Hai, Thứ Tư, Thứ Sáu
    // syntax : giây phút giờ mọi ngày mọi tháng thứ 2,4,6
    @Scheduled(cron = "0 12 14 * * 1,3,5")
    public void TaskScheduler() {
                executeTask();
    }

    public void retryTask() {
        while (retryCount < 5) {
            try {
                retryCount++;
                System.out.println("Retry attempt " + retryCount + "...");
                Thread.sleep(delay);
                executeTask();
                System.out.println("Task executed successfully on attempt " + retryCount);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                if (retryCount == 5) {
                    System.out.println("Maximum retry attempts reached. Stopping.");
                    break;
                }
            }
        }
    }

    public void executeTask() {
        try {
            System.out.println("Scheduled task executed at: " + LocalDateTime.now());
            runExternalExecutable();
        } catch (IOException | SQLException | MessagingException e) {
            System.out.println("Error! Check your file script or database");
            e.printStackTrace();
            retryTask();
        }
    }

    public void runExternalExecutable() throws IOException, MessagingException, SQLException {
        ProcessBuilder processBuilder = new ProcessBuilder("java","-jar", pathScript);
        processBuilder.redirectErrorStream(true);
        System.out.println("Loading...");
        // execute file script to crawl data
        Process process = processBuilder.start();

        // print execution progress
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
            System.out.println("jar script executed with exit code: " + exitCode);
            // Check status crawl
            if (exitCode == 0) {
                System.out.println("Script executed successfully!");
            } else {
                System.out.println("Script execution failed with exit code: " + exitCode);
                throw new IOException();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException();
        }
    }

    public static void main(String[] args) {
        System.out.println(LocalDateTime.now());
    }
}
