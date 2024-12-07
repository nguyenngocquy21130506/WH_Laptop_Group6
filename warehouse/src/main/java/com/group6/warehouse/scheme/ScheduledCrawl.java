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
public class ScheduledCrawl {

    @Value("${email.user.hostA}")
    String emailA;
    Connection connection;

    @Autowired
    SendMail sendMail;

    @Value("${custom.file.crawlPath}")
    private String pathScript;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${retry.attempts}")
    private int retryCount;

    @Value("${retry.delay}")
    private int delay;

    private int numRetry;

    // Chạy vào lúc 0:00:00 các ngày Thứ Hai, Thứ Tư, Thứ Sáu
    // syntax : <giây> <phút> <giờ> <mọi ngày trong tháng> <mọi tháng trong năm> <thứ 2,4,6>
    @Scheduled(cron = "0 15 11 * * 1,3,5")
    public void TaskScheduler() {
        System.out.println("------------------CRAWL DATA------------------");
        // 1. Thực thi hàm init() để kết nối database
        init();
        // 2.2 Lấy dữ liệu từ bảng logs xem đã lấy dữ liệu ngày hôm nay chưa
        try {
            PreparedStatement stmt = connection.prepareStatement("select id from logs where id_config = ? and status = ? and year(created_at) = ? and month(created_at) = ? and day(created_at) = ?");
            stmt.setInt(1, 1);
            stmt.setString(2, "Success");
            stmt.setInt(3, LocalDateTime.now().getYear());
            stmt.setInt(4, LocalDateTime.now().getMonthValue());
            stmt.setInt(5, LocalDateTime.now().getDayOfMonth());
            ResultSet rs = stmt.executeQuery();
            // 3. Kiểm tra dữ liệu hôm nay đã có
            if (rs.next()) {
                System.out.println("Exist data of today! Check your database or network");
            } else {
                System.out.println("Task started at: " + LocalDateTime.now());
                // 4. Thực thi hàm executeTask()
                executeTask();
            }
        } catch (SQLException e) {
            System.out.println("Exist data of today! Check your database or network");
            throw new RuntimeException(e);
        }
    }

    public void retryTask() {
        // 8.1 Kiểm tra số lần chạy lại tiến trình
        while (numRetry < retryCount) {
            try {
                numRetry++;
                System.out.println("Retry attempt " + numRetry + "...");
                Thread.sleep(delay);
                // 4. Thực thi hàm executeTask()
                executeTask();
                System.out.println("Task executed successfully on attempt " + numRetry);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                if (numRetry == 5) {
                    System.out.println("Maximum retry attempts reached. Stopping.");
                    break;
                }
            }
        }
    }

    public void executeTask() {
        try {
            System.out.println("Scheduled task executed at: " + LocalDateTime.now());
            // 5. Thực thi hàm runExternalExcutable()
            runExternalExecutable();
        } catch (IOException | SQLException | MessagingException e) {
            System.out.println("Error! Check your file script or database");
            e.printStackTrace();
            // 8. Thực thi hàm retryTask()
            retryTask();
        }
    }

    public void runExternalExecutable() throws IOException, MessagingException, SQLException {
        DataFileConfig dataFileConfig = null;
        int numRow = 0;
        try (PreparedStatement stmt = connection.prepareStatement("SELECT count(id) FROM data_file_configs")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                numRow++;
            }
        }
        System.out.println("số nguồn dữ liệu: " + numRow);
        for (int i = 1; i <= numRow; i++) {
            try {
                // 6. Lấy dữ liệu cấu hình từ data_file_configs để truyền vào script
                String sql = "SELECT * FROM data_file_configs WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setLong(1, i);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        dataFileConfig = new DataFileConfig();
                        dataFileConfig.setName(rs.getString("name"));
                        dataFileConfig.setDirectoryFile(rs.getString("directory_file"));
                        dataFileConfig.setFilename(rs.getString("filename"));
                        dataFileConfig.setFormat(rs.getString("format"));
                    }
                }
                // 7. Kiểm tra lấy dữ liệu thành công
            } catch (Exception e) {
                Log log = Log.builder()
                        .idConfig(1L)
                        .taskName("Get data config")
                        .status("Fail")
                        .message("Get data config failed! Check your database or network")
                        .level(LevelEnum.ERROR.getValue())
                        .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                        .endTime(Timestamp.valueOf(LocalDateTime.now()))
                        .build();
                try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO logs (id_config, task_name, status, message, level, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                    stmt.setLong(1, log.getIdConfig());
                    stmt.setString(2, log.getTaskName());
                    stmt.setString(3, log.getStatus());
                    stmt.setString(4, log.getMessage());
                    stmt.setInt(5, log.getLevel());
                    stmt.setTimestamp(6, log.getCreatedAt());
                    stmt.setTimestamp(7, log.getEndTime());
                    stmt.executeUpdate();
                }
                // 8.1 Kiểm tra số lần chạy lại tiến trình
                if (retryCount == 5) {
                    // 8.1.1 Gửi mail về với taskname tương ứng và status "Fail"
                    sendMail.sendEmail(emailA, log);
                }
                e.printStackTrace();
                throw new SQLException();
            }
            // 7.1 Xử lý các dữ liệu được lấy ra để làm dữ liệu truyền vào script
            LocalDate today = LocalDate.now();
            String date = String.format("%d_%d_%d", today.getDayOfMonth(), today.getMonthValue(), today.getYear());
            String pathDestination = dataFileConfig.getDirectoryFile();
            String fileDestination = dataFileConfig.getFilename() + dataFileConfig.getName().toUpperCase() + "_" + date + "." + dataFileConfig.getFormat().toUpperCase();

            // 8. Thực thi file script bằng ProcessBuilder với tham số là dường dẫn lưu file và tên file dữ liệu
            ProcessBuilder processBuilder = null;
            processBuilder = new ProcessBuilder("python", pathScript, pathDestination, fileDestination);
            processBuilder.redirectErrorStream(true);
            System.out.println("Loading...");
            Process process = processBuilder.start();
            // 9. Lưu logs với trạng thái Running vào database
            Log log = Log.builder()
                    .idConfig(1L)
                    .taskName("Crawl data")
                    .status("Running")
                    .message("Crawl data from Tiki.vn")
                    .level(LevelEnum.INFO.getValue())
                    .build();
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO logs (id_config, task_name, status, message, level, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setLong(1, log.getIdConfig());
                stmt.setString(2, log.getTaskName());
                stmt.setString(3, log.getStatus());
                stmt.setString(4, log.getMessage());
                stmt.setInt(5, log.getLevel());
                stmt.setTimestamp(6, log.getCreatedAt());
                stmt.executeUpdate();
            }
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
                // 12 Kiểm tra lưu file thành công
                if (exitCode == 0) {
                    log.setStatus("Success");
                    log.setEndTime(Timestamp.valueOf(LocalDateTime.now()));
                    // 12.1 Lưu logs với trạng thái Success vào database
                    try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO logs (id_config, task_name, status, message, level, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                        stmt.setLong(1, log.getIdConfig());
                        stmt.setString(2, log.getTaskName());
                        stmt.setString(3, log.getStatus());
                        stmt.setString(4, log.getMessage());
                        stmt.setInt(5, log.getLevel());
                        stmt.setTimestamp(6, log.getCreatedAt());
                        stmt.setTimestamp(7, log.getEndTime());
                        stmt.executeUpdate();
                    }
                    // 13. Gửi mail về với taskname "Get data config" và status "Success"
                    sendMail.sendEmail(emailA, log);
                    System.out.println("Script executed successfully!");
                } else {
                    log.setStatus("Fail");
                    log.setLevel(LevelEnum.ERROR.getValue());
                    // 12.2 Lưu logs với trạng thái Fail vào database
                    try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO logs (id_config, task_name, status, message, level, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                        stmt.setLong(1, log.getIdConfig());
                        stmt.setString(2, log.getTaskName());
                        stmt.setString(3, log.getStatus());
                        stmt.setString(4, log.getMessage());
                        stmt.setInt(5, log.getLevel());
                        stmt.setTimestamp(6, log.getCreatedAt());
                        stmt.setTimestamp(7, log.getEndTime());
                        stmt.executeUpdate();
                    }
                    System.out.println("Script execution failed with exit code: " + exitCode);
                    // 8.1 Kiểm tra số lần chạy lại tiến trình
                    if (retryCount == 5) {
                        sendMail.sendEmail(emailA, log);
                    }
                    throw new IOException();
                }
            } catch (InterruptedException e) {
                log.setStatus("Fail");
                log.setLevel(LevelEnum.ERROR.getValue());
                try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO logs (id_config, task_name, status, message, level, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                    stmt.setLong(1, log.getIdConfig());
                    stmt.setString(2, log.getTaskName());
                    stmt.setString(3, log.getStatus());
                    stmt.setString(4, log.getMessage());
                    stmt.setInt(5, log.getLevel());
                    stmt.setTimestamp(6, log.getCreatedAt());
                    stmt.setTimestamp(7, log.getEndTime());
                    stmt.executeUpdate();
                }
                try {
                    sendMail.sendEmail(emailA, log);
                } catch (MessagingException ex) {
                    System.out.println("Send mail error! Check your email or network");
                    e.printStackTrace();
                }
                e.printStackTrace();
                throw new IOException();
            }
        }
    }

    //    @PostConstruct
    public void init() {
        System.out.println("Start connect to DB");
        try {
            // 1.1 Kết nối database với username: root, password:""
            this.connection = DriverManager.getConnection(url, username, password);
            // 2. Kiểm tra kết nối thành công
        } catch (SQLException e) {
            Log log = Log.builder()
                    .idConfig(1L)
                    .taskName("Connect DB Control")
                    .status("Fail")
                    .message("Connect to database control failed! Check your database or network")
                    .level(LevelEnum.ERROR.getValue())
                    .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                    .endTime(Timestamp.valueOf(LocalDateTime.now()))
                    .build();
            try {
                // 2.1 Gửi mail về với taskname là "Connect DB Control" và status "Fail"
                sendMail.sendEmail(emailA, log);
            } catch (MessagingException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        System.out.println(LocalDateTime.now());
    }
}

