package org.example;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Backup1 {

    private static String CONTROL_DB_URL;
    private static String WAREHOUSE_DB_URL;
    private static String STAGING_DB_URL;
    private static String USER;
    private static String PASSWORD;
    private static String SMTP_HOST;
    private static String SMTP_PORT;
    private static String EMAIL_USER;
    private static String EMAIL_PASSWORD;
    private static String EMAIL_FROM;
    private static String EMAIL_TO;

    // Phương thức để tải cấu hình từ file config.properties
    public static void loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("D:\\workspace\\DataWare\\TransformDataScript\\src\\main\\resources\\config.properties")) {
            properties.load(input);

            CONTROL_DB_URL = properties.getProperty("CONTROL_DB_URL");
            WAREHOUSE_DB_URL = properties.getProperty("WAREHOUSE_DB_URL");
            STAGING_DB_URL = properties.getProperty("STAGING_DB_URL");
            USER = properties.getProperty("DB_USER");
            PASSWORD = properties.getProperty("DB_PASSWORD");

            SMTP_HOST = properties.getProperty("SMTP_HOST");
            SMTP_PORT = properties.getProperty("SMTP_PORT");
            EMAIL_USER = properties.getProperty("EMAIL_USER");
            EMAIL_PASSWORD = properties.getProperty("EMAIL_PASSWORD");
            EMAIL_FROM = properties.getProperty("EMAIL_FROM");
            EMAIL_TO = properties.getProperty("EMAIL_TO");

            System.out.println("Cấu hình đã được tải thành công từ file config.properties.");


        } catch (IOException e) {
            System.out.println("Không thể tải file cấu hình: " + e.getMessage());
        }
    }
    public void sendEmail(String subject, String messageContent) {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // Tạo session cho email
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_USER, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO));
            message.setSubject(subject);
            message.setText(messageContent);

            // Gửi email
            Transport.send(message);

            System.out.println("Email đã được gửi thành công.");
        } catch (MessagingException e) {
            System.out.println("Không thể gửi email: " + e.getMessage());
        }
    }


    private Connection connectToControlDB() throws SQLException {
        return DriverManager.getConnection(CONTROL_DB_URL, USER, PASSWORD);
    }

    private Connection connectToWarehouseDB() throws SQLException {
        return DriverManager.getConnection(WAREHOUSE_DB_URL, USER, PASSWORD);
    }

    private Connection connectToStagingDB() throws SQLException {
        return DriverManager.getConnection(STAGING_DB_URL, USER, PASSWORD);
    }
    // 1. Load config properties
    // Lấy tên bảng từ table_config
    public String getTableName(String configId) {
        String tableName = "";
        String sql = "SELECT name_table FROM table_config WHERE id = ?";
        try (Connection conn = connectToControlDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, configId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                tableName = rs.getString("name_table");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tableName;
    }

    // Lấy danh sách cột và kiểu dữ liệu từ detail_table
    public List<String[]> getColumnsAndTypes(String configId) {
        List<String[]> columns = new ArrayList<>();
        String sql = "SELECT name_column, data_type FROM detail_table WHERE id_table_config = ?";
        try (Connection conn = connectToControlDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, configId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String nameColumn = rs.getString("name_column");
                String dataType = rs.getString("data_type");
                columns.add(new String[]{nameColumn, dataType});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return columns;
    }

    // Chuyển đổi dữ liệu từ staging_laptop sang staging_laptop_transform
    public void transformDataToLaptopTransform() {
        String configId = "staging_laptop_transform_n";
        String tableName = "staging." + getTableName(configId);
        List<String[]> columns = getColumnsAndTypes(configId);

        StringBuilder selectColumns = new StringBuilder();
        for (String[] column : columns) {
            if (selectColumns.length() > 0) {
                selectColumns.append(", ");
            }

            String columnName = column[0];
            String dataType = column[1];

            // Kiểm tra kiểu dữ liệu và xử lý giá trị rỗng
            if (dataType.equalsIgnoreCase("INT")) {
                selectColumns.append("IFNULL(CAST(NULLIF(").append(columnName).append(", '') AS UNSIGNED), 0)");
            } else if (dataType.equalsIgnoreCase("DATETIME")) {
                selectColumns.append("IFNULL(").append(columnName).append(", CURRENT_TIMESTAMP())");
            } else {
                selectColumns.append("NULLIF(").append(columnName).append(", '')");
            }
        }

        String transformSql = "INSERT INTO " + tableName + " (" + getColumnNames(columns) + ") " +
                "SELECT " + selectColumns + " FROM staging.staging_laptop";

        try (Connection conn = connectToStagingDB();
             PreparedStatement pstmt = conn.prepareStatement(transformSql)) {

            int rowsInserted = pstmt.executeUpdate();
            System.out.println("Rows transformed and inserted into " + tableName + ": " + rowsInserted);

        } catch (SQLException e) {
            System.out.println("Error transforming data into " + tableName + ": " + e.getMessage());
        }
    }


    private String getColumnNames(List<String[]> columns) {
        StringBuilder columnNames = new StringBuilder();
        for (String[] column : columns) {
            if (columnNames.length() > 0) {
                columnNames.append(", ");
            }
            columnNames.append(column[0]);
        }
        return columnNames.toString();
    }

    // Chuyển dữ liệu từ staging_laptop_transform sang brand_dim
    private void migrateToBrandDim() {
        String configId = "brand_dim_n";  // ID cấu hình trong bảng control cho brand_dim
        String tableName = getTableName(configId);  // Lấy tên bảng brand_dim từ table_config
        List<String[]> columns = getColumnsAndTypes(configId);  // Lấy danh sách cột từ detail_table

        // Lấy danh sách các tên cột, không trùng lặp cho câu lệnh SELECT và INSERT
        String columnNames = getColumnNames(columns);

        // Tạo câu lệnh INSERT, đảm bảo không có dữ liệu trùng lặp
        String insertSql = "INSERT INTO datawarehouse." + tableName + " (" + columnNames + ",dt_expire) " +
                "SELECT DISTINCT " + columnNames + ",'9999-12-31' FROM staging.staging_laptop_transform s " +
                "ON DUPLICATE KEY UPDATE brand_name = VALUES(brand_name)";

        // Thực hiện chèn dữ liệu
        executeInsert(insertSql, tableName);
    }





    // Chuyển dữ liệu từ staging_laptop_transform sang product_dim
    private void migrateToProductDim() {
        String configId = "product_dim_n";
        String tableName = getTableName(configId);
        List<String[]> columns = getColumnsAndTypes(configId);

        String columnNames = getColumnNames(columns);

        // Truy vấn INSERT cho product_dim với điều kiện kiểm tra khóa ngoại brand_id
        String insertSql = "INSERT INTO datawarehouse." + tableName + " (" + columnNames + ",dt_expire) " +
                "SELECT " + columnNames + ",'9999-12-31' FROM staging.staging_laptop_transform s ";

        executeInsert(insertSql, tableName);
    }


    // Chuyển dữ liệu từ staging_laptop_transform sang date_dim
    private void migrateToDateDim() {
        String configId = "date_dim_n";
        String tableName = getTableName(configId);
        List<String[]> columns = getColumnsAndTypes(configId);

        String columnNames = getColumnNames(columns);

        // Truy vấn INSERT cho date_dim
        String insertSql = "INSERT INTO datawarehouse." + tableName + " (" + columnNames + ") " +
                "SELECT DISTINCT DATE(s.created_at) AS full_date, " +
                "DAYOFWEEK(s.created_at) AS day_of_week, MONTH(s.created_at) AS month, YEAR(s.created_at) AS year " +
                "FROM staging.staging_laptop_transform s " +
                "WHERE NOT EXISTS (SELECT 1 FROM datawarehouse." + tableName + " d WHERE d.full_date = DATE(s.created_at))";

        executeInsert(insertSql, tableName);
    }


    private void executeInsert(String sql, String tableName) {
        try (Connection conn = connectToWarehouseDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int rowsInserted = pstmt.executeUpdate();
            System.out.println("Rows inserted into " + tableName + ": " + rowsInserted);
        } catch (SQLException e) {
            System.out.println("Error inserting data into " + tableName + ": " + e.getMessage());
        }
    }

    private boolean checkControlLog() {
        String sql = "SELECT * FROM logs WHERE status = 'SUCCESS'";
        try (Connection conn = connectToControlDB();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Error checking control log: " + e.getMessage());
            return false;
        }
    }

    private void logResult(int idConfig, String taskName, String status, String message, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "INSERT INTO logs (id_config, task_name, status, message, end_time, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectToControlDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idConfig);
            pstmt.setString(2, taskName);
            pstmt.setString(3, status);
            pstmt.setString(4, message);
            pstmt.setTimestamp(5, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();
            System.out.println("Log entry successfully added to logs table.");
            // Gửi email sau khi ghi log thành công
            String subject = "Data Migration Log - " + status;
            String emailContent = "Task: " + taskName + "\nStatus: " + status + "\nMessage: " + message +
                    "\nStart Time: " + startTime + "\nEnd Time: " + endTime;
            sendEmail(subject, emailContent);
        } catch (SQLException e) {
            System.out.println("Error logging result: " + e.getMessage());
        }
    }

    // Phương thức kiểm tra trạng thái Crawl data
    private boolean checkCrawlDataToStagingStatus() {
        String sql = "SELECT end_time FROM logs " +
                "WHERE message = ? AND status = ? " +
                "ORDER BY end_time DESC LIMIT 1";
        try (Connection conn = connectToControlDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "Crawl data from Tiki.vn");
            pstmt.setString(2, "Success");
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Đã có một bản ghi thành công với thời gian gần nhất
                System.out.println("Crawl data status is successful. Proceeding...");
                return true;
            } else {
                System.out.println("No successful recent crawl data record found.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error checking crawl data status: " + e.getMessage());
            return false;
        }
    }
    // Phương thức kiểm tra kết nối đến DB Staging
    public boolean checkStagingDBConnection() {
        try (Connection conn = connectToStagingDB()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Kết nối đến DB Staging thành công.");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Không thể kết nối đến DB Staging.");
            logResult(1, "CheckStagingDBConnection", "FAILED", "Không thể kết nối đến DB Staging: " + e.getMessage(), LocalDateTime.now(), LocalDateTime.now());
        }
        return false;
    }
    // Phương thức kiểm tra kết nối đến DB Data Warehouse
    public boolean checkWarehouseDBConnection() {
        try (Connection conn = connectToWarehouseDB()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Kết nối đến DB Data Warehouse thành công.");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Không thể kết nối đến DB Data Warehouse.");
            logResult(1, "CheckWarehouseDBConnection", "FAILED", "Không thể kết nối đến DB Data Warehouse: " + e.getMessage(), LocalDateTime.now(), LocalDateTime.now());
        }
        return false;
    }
    // Phương thức rerun để thực hiện lại quy trình với một ngày và tên script nhập vào
//    public void rerunDataMigration() {
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.println("Nhập ngày để rerun (yyyy-MM-dd): ");
//        String date = scanner.nextLine();
//
//        System.out.println("Nhập tên script (ví dụ: dataMigration): ");
//        String scriptName = scanner.nextLine();
//
//        LocalDateTime startTime = LocalDateTime.now();
//
//        try {
//            // Đặt is_delete = 1 cho các bản ghi trùng ngày trong product_dim và brand_dim
//            markOldRecordsAsDeleted("product_dim", date);
//            markOldRecordsAsDeleted("brand_dim", date);
//
//            // Thực hiện lại quá trình migration
//            System.out.println("Thực hiện rerun cho " + scriptName + "...");
//            executeDataWorkflow();
//
//            // Ghi log rerun thành công
//            logResult(1, scriptName, "SUCCESS", "Rerun data migration thành công cho ngày " + date, startTime, LocalDateTime.now());
//
//        } catch (Exception e) {
//            System.out.println("Có lỗi xảy ra khi rerun data migration.");
//            logResult(1, scriptName, "FAILED", "Rerun data migration thất bại: " + e.getMessage(), startTime, LocalDateTime.now());
//            e.printStackTrace();
//        }
//    }

    // Phương thức đánh dấu các bản ghi có ngày trùng với is_delete = 1
//    private void markOldRecordsAsDeleted(String tableName, String date) {
//        String updateSql = "UPDATE datawarehouse." + tableName + " SET is_delete = 1 WHERE DATE(created_at) = ?";
//
//        try (Connection conn = connectToWarehouseDB();
//             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
//            pstmt.setString(1, date);
//            int rowsUpdated = pstmt.executeUpdate();
//            System.out.println("Đã đánh dấu " + rowsUpdated + " bản ghi trong " + tableName + " là is_delete = 1 cho ngày " + date);
//        } catch (SQLException e) {
//            System.out.println("Lỗi khi đánh dấu các bản ghi trong " + tableName + ": " + e.getMessage());
//        }
//    }

    // Cập nhật executeDataWorkflow để kiểm tra trước khi thực thi
    public void executeDataWorkflow() {
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("Bắt đầu thực thi workflow...");

        try {
            // Kiểm tra trạng thái của crawl data
            if (!checkCrawlDataToStagingStatus()) {
                logResult(1, "Load", "FAILED", "Load data was not successful recently", startTime, LocalDateTime.now());
                System.out.println("Dừng lại do tải dữ không thành công gần đây.");
                return;
            }

            // Tiếp tục quy trình nếu trạng thái hợp lệ
            int idConfig = 1;
            String taskName = "LoadFromStagingToWarehouse";

            System.out.println("Bắt đầu chuyển đổi dữ liệu...");
            transformDataToLaptopTransform();

            System.out.println("Di chuyển dữ liệu sang Brand Dim...");
            migrateToBrandDim();

            System.out.println("Di chuyển dữ liệu sang Product Dim...");
            migrateToProductDim();

            System.out.println("Di chuyển dữ liệu sang Date Dim...");
            migrateToDateDim();

            // Ghi log kết quả thành công
            logResult(idConfig, taskName, "SUCCESS", "Data transferred successfully", startTime, LocalDateTime.now());

        } catch (Exception e) {
            System.out.println("Có lỗi xảy ra khi thực thi workflow.");
            logResult(1, "LoadFromStagingToWarehouse", "FAILED", e.getMessage(), startTime, LocalDateTime.now());
            e.printStackTrace();
        }
        System.out.println("Kết thúc thực thi workflow.");
    }


//    public static void main(String[] args) {
//        // Tải cấu hình trước khi sử dụng các phương thức kết nối
//        loadConfig();
//
//        DataMigration migration = new DataMigration();
//        try {
//            if (migration.connectToControlDB() != null) {
//                System.out.println("Kết nối đến Control DB thành công.");
//            }
//            if (migration.connectToWarehouseDB() != null) {
//                System.out.println("Kết nối đến Warehouse DB thành công.");
//            }
//            if (migration.connectToStagingDB() != null) {
//                System.out.println("Kết nối đến Staging DB thành công.");
//            }
//        } catch (SQLException e) {
//            System.out.println("Lỗi kết nối đến DB: " + e.getMessage());
//        }
//        migration.executeDataWorkflow();
//    }
}
