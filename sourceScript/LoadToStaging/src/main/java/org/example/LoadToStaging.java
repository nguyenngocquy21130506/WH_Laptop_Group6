package org.example;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoadToStaging {
    public static void main(String[] args) {
        // Bước 1: Đọc cấu hình từ file properties
        final String DB_CONTROL_URL = ConfigReader.get("db.control.url");
        final String DB_CONTROL_USER = ConfigReader.get("db.control.username");
        final String DB_CONTROL_PASSWORD = ConfigReader.get("db.controlpassword");

        final String DB_STAGING_URL = ConfigReader.get("db.staging.url");
        final String DB_STAGING_USER = ConfigReader.get("db.staging.username");
        final String DB_STAGING_PASSWORD = ConfigReader.get("db.staging.password");

        final String FROM_EMAIL = ConfigReader.get("email.from");
        final String EMAIL_PASSWORD = ConfigReader.get("email.password");
        final String TO_EMAIL = ConfigReader.get("email.to");

        Connection dbControlConnection = null;
        Connection dbStagingConnection = null;
        Statement statement = null;
        ResultSet configResultSet = null;
        long idConfig = 1;

        // Tạo đối tượng EmailSender
        EmailSender emailSender = new EmailSender(FROM_EMAIL, EMAIL_PASSWORD);
        LocalDate dateToLoad;
        // Kiểm tra tham số ngày
        if (args.length > 0 && !args[0].trim().isEmpty()) {
            try {
                // Định dạng ngày từ tham số dòng lệnh
                dateToLoad = LocalDate.parse(args[0], DateTimeFormatter.ofPattern("dd_MM_yyyy"));
                System.out.println("Loading data for specified date: " + dateToLoad);
            } catch (Exception e) {
                System.err.println("Invalid date format. Please use 'dd_MM_yyyy'. Example: 01_11_2024");
                return;
            }
        } else {
            // Nếu không có tham số ngày thì sử dụng ngày hiện tại
            dateToLoad = LocalDate.now();
            System.out.println("No date specified. Using current date: " + dateToLoad);
        }

        // Kiểm tra tham số id_config
        if (args.length > 1 && !args[1].trim().isEmpty()) {
            try {
                // Gán giá trị cho idConfig nếu tham số id_config hợp lệ
                idConfig = Integer.parseInt(args[1]);
                System.out.println("Loading data for id_config: " + idConfig);
            } catch (NumberFormatException e) {
                System.err.println("Invalid id_config format. It should be an integer.");
                return;
            }
        } else {
            // Nếu không có tham số id_config thì sử dụng giá trị mặc định là 1
            System.out.println("No id_config specified. Using default id_config: " + idConfig);
        }
        try {
            // Bước 2: Kết nối tới DBControl
            dbControlConnection = DriverManager.getConnection(DB_CONTROL_URL, DB_CONTROL_USER, DB_CONTROL_PASSWORD);
            // Bước 3: Kiểm tra kết nối db control thành công
            System.out.println("Connection to DBControl successful.");
            // Bước 4: Kiểm tra trạng thái của LoadToStaging theo ngày và idConfig
            String checkStatusQuery = "SELECT status FROM logs WHERE id_config = ? AND task_name = 'LoadToStaging' AND DATE(end_time) = ? ORDER BY end_time DESC LIMIT 1";
            try (PreparedStatement checkStatement = dbControlConnection.prepareStatement(checkStatusQuery)) {
                checkStatement.setLong(1, idConfig);
                checkStatement.setDate(2, Date.valueOf(dateToLoad));
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (resultSet.next()) {
                        String status = resultSet.getString("status");
                        // Dừng script nếu đã load thành công
                        if ("Success".equalsIgnoreCase(status)) {
                            System.out.println("LoadToStaging already completed successfully for the specified date. Exiting.");
                            return;
                        }
                    }
                }
            }
            // Bước 5: Ghi status vào bảng logs là running
            logToLogTable(dbControlConnection, idConfig, "LoadToStaging", "Running", "Script started.", "0", LocalDateTime.now());
        }
        catch (SQLException e) {
            // Bước 3.1: Kết nối DBControl thất bại gửi mail
            System.err.println("Failed to connect to DBControl: " + e.getMessage());
            emailSender.sendEmail(TO_EMAIL, "Connect to database control", "Connect to database control is failed.");
            return;
        }
        try {
            //Bước 6: Lấy ra directoryFile và filename
            String crawlDetails = getCrawlFileDetails(dbControlConnection, dateToLoad); // Ví dụ trả về "Directory: D:\\data_warehouse, Filename: crawled_data_laptop_"
            // Tách chuỗi crawlDetails thành một mảng
            String[] details = crawlDetails.split(", ");
            // Kiểm tra xem mảng có đủ 2 phần tử không (directory_file và filename)
            if (details.length < 3) {
                // Nếu không đủ 2 phần tử, ghi log và dừng chương trình lại
                logToLogTable(dbControlConnection, idConfig, "LoadToStaging", "Fail", "CrawlData not success. Missing directory_file or filename.", "1", LocalDateTime.now());
                emailSender.sendEmail(TO_EMAIL, "LoadToStaging", "CrawlData not success. Missing directory_file or filename.");
                return;
            }
            // Nếu mảng có đủ 2 phần tử, tiếp tục xử lý
            String directoryFile = details[0].replace("Directory: ", ""); // Phần tử đầu tiên của chuỗi
            String filename = details[1].replace("Filename: ", "");
            String name = details[2].replace("Name: ", ""); // Phần tử thứ 2 của chuỗi// Phần tử thứ 2 của chuỗi

            // Lấy ngày hiện tại và định dạng
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy");
            String formattedDate = dateToLoad.format(formatter);

            // Bước 7: Lấy đường dẫn tệp hoàn chỉnh
            String filePath = String.format("%s\\%s%s_%s.csv", directoryFile, filename,name.toUpperCase(), formattedDate);
            System.out.println("Full CSV file path: " + filePath);

            // Bước 8: Kết nối tới DBStaging
            try {
                dbStagingConnection = DriverManager.getConnection(DB_STAGING_URL, DB_STAGING_USER, DB_STAGING_PASSWORD);
                // Bước 9: Kiểm tra kết nối db staging, thành công thì qua bước 11
                System.out.println("Connected to DBStaging successfully.");
            } catch (SQLException e) {
                // Bước 9.1: Kết nối DB Staging thất bại, ghi log và gửi mail
                System.err.println("Failed to connect to DBStaging: " + e.getMessage());
                logToLogTable(dbControlConnection, idConfig, "LoadToStaging", "Fail", "Failed to connect to DB Staging", "1",LocalDateTime.now());
                emailSender.sendEmail(TO_EMAIL, "Connect database staging", "Fail to connect to DB Staging.");
                return;
            }
            // Bước 10: Kiểm tra nếu bảng staging_laptop có dữ liệu hay không
            statement = dbStagingConnection.createStatement();
            String checkDataQuery = "SELECT COUNT(*) FROM staging_laptop;";
            ResultSet countResultSet = statement.executeQuery(checkDataQuery);
            int rowCount = 0;
            if (countResultSet.next()) {
                rowCount = countResultSet.getInt(1);
            }

            // Bước 10.1: Nếu có dữ liệu, xóa tất cả
            if (rowCount > 0) {
                String deleteQuery = "DELETE FROM staging_laptop;";
                statement.executeUpdate(deleteQuery);
                System.out.println("Existing data in staging_laptop has been deleted.");
            }

            // Bước 11: Đọc và tải dữ liệu vào bảng staging_laptop
            String tableName= "";
            String columns = "";
            String query = "Select name_table, columns from table_configs where id = 'staging_laptop_n'";
            try (PreparedStatement configStatement = dbControlConnection.prepareStatement(query);
                 ResultSet resultSet = configStatement.executeQuery()) {
                if (resultSet.next()) {
                    tableName = resultSet.getString("name_table");
                    columns = resultSet.getString("columns");
                }
            } catch (SQLException e) {
                System.err.println("Lỗi khi lấy cấu hình bảng: " + e.getMessage());
                logToLogTable(dbControlConnection, idConfig, "LoadToStaging", "Fail", "Lỗi khi lấy cấu hình bảng: " + e.getMessage(), "1",LocalDateTime.now());
                return; // Thoát nếu có lỗi
            }
            String loadDataQuery = String.format(
                    "LOAD DATA INFILE '%s' " +
                            "INTO TABLE %s " +
                            "FIELDS TERMINATED BY ';' " +
                            "ENCLOSED BY '\"' " +
                            "LINES TERMINATED BY '\\n' " +
                            "IGNORE 1 ROWS " +
                            "(%s);",
                    filePath.replace("\\", "\\\\"),
                    tableName,
                    columns
            );
            System.out.println("Executing query: " + loadDataQuery);

            // Thực thi câu lệnh
            statement = dbStagingConnection.createStatement();
            statement.execute(loadDataQuery);
            System.out.println("Data loaded to staging_laptop successfully.");
            // Bước 12.1: Ghi log và gửi mail khi thành công
            logToLogTable(dbControlConnection, idConfig, "LoadToStaging", "Success", "Data loaded successfully into staging_laptop.", "0", LocalDateTime.now());
            emailSender.sendEmail(TO_EMAIL, "LoadToStaging Success", "Data loaded successfully into staging_laptop.");
        } catch (SQLException e) {
            System.err.println("Database connection or execution error: " + e.getMessage());
            // Bước 12.2: Ghi log và gửi mail khi thất bại
            if (dbControlConnection != null) {
                logToLogTable(dbControlConnection, idConfig, "LoadToStaging", "Fail", "Error occurred: " + e.getMessage(), "1", LocalDateTime.now());
                emailSender.sendEmail(TO_EMAIL, "Error in LoadToStaging", "Data loaded fail into staging_laptop");
            }
        } finally {
            // Bước 13: Đóng kết nối và tài nguyên
            try {
                if (configResultSet != null) configResultSet.close();
                if (statement != null) statement.close();
                if (dbControlConnection != null) dbControlConnection.close();
                if (dbStagingConnection != null) dbStagingConnection.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
     //Phương thức kiểm tra trạng thái của crawl dữ liệu theo ngày chỉ định
    private static String getCrawlFileDetails(Connection dbControlConnection, LocalDate dateToCheck) {
        // Truy vấn để kiểm tra log crawl dữ liệu thành công vào ngày chỉ định và lấy directory_file, filename từ data_file_configs
        String logQuery = "SELECT df.directory_file, df.filename,df.name "
                + "FROM logs l "
                + "JOIN data_file_configs df ON l.id_config = df.id "
                + "WHERE DATE(l.created_at) = ? AND l.status = 'Success' AND l.task_name = 'CrawlData' AND l.id_config = 1";

        try (PreparedStatement statement = dbControlConnection.prepareStatement(logQuery)) {
            statement.setDate(1, Date.valueOf(dateToCheck));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // Lấy các giá trị từ resultSet
                    String directoryFile = resultSet.getString("directory_file");
                    String filename = resultSet.getString("filename");
                    String sourcename = resultSet.getString("name");

                    // Trả về thông tin dưới dạng String (có thể kết hợp chúng trong một chuỗi)
                    return "Directory: " + directoryFile + ", Filename: " + filename + ", Name: " + sourcename;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking crawl logs: " + e.getMessage());
        }
        // Nếu không có kết quả thỏa mãn, trả về thông báo lỗi hoặc chuỗi rỗng
        return "No successful crawl data found for the given date.";
    }
    // Phương thức ghi log vào bảng logs
    private static void logToLogTable(Connection dbControlConnection, long idConfig, String taskName, String status, String message, String level, LocalDateTime endTime) {
        String logQuery = "INSERT INTO logs (id_config, task_name, status, message, level, end_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement logStatement = dbControlConnection.prepareStatement(logQuery)) {
            logStatement.setLong(1, idConfig);
            logStatement.setString(2, taskName);
            logStatement.setString(3, status);
            logStatement.setString(4, message);
            logStatement.setString(5, level);
            logStatement.setTimestamp(6, Timestamp.valueOf(endTime));
            logStatement.executeUpdate();
            System.out.println("Log entry added to Log: " + status);
        } catch (SQLException e) {
            System.err.println("Error logging to Log table: " + e.getMessage());
        }
    }
}
