import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.UUID;


public class DataMigration {

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


    private  static String path;

    private final Map<String, List<String[]>> tableColumnMap = new HashMap<>();

    // Tải cấu hình từ file config.properties
    public static void loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("D:\\ServerWH\\WH_Laptop_Group6\\sourceScript\\TransformDataScript\\src\\main\\resources\\config.properties")) {
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
//2. Kết nối database control.db
    private Connection connectToControlDB() throws SQLException {
        return DriverManager.getConnection(CONTROL_DB_URL, USER, PASSWORD);
    }

    private Connection connectToWarehouseDB() throws SQLException {
        return DriverManager.getConnection(WAREHOUSE_DB_URL, USER, PASSWORD);
    }

    private Connection connectToStagingDB() throws SQLException {
        return DriverManager.getConnection(STAGING_DB_URL, USER, PASSWORD);
    }
    public boolean checkStagingDBConnection() {
        try (Connection conn = connectToStagingDB()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Kết nối đến DB Staging thành công.");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Không thể kết nối đến DB Staging.");
            logResult(1, "CheckStagingDBConnection", "", "Không thể kết nối đến DB Staging: " + e.getMessage(), LocalDateTime.now(), LocalDateTime.now());
        }
        return false;
    }
    public boolean checkWarehouseDBConnection() {
        try (Connection conn = connectToWarehouseDB()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Kết nối đến DB Data Warehouse thành công.");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Không thể kết nối đến DB Data Warehouse.");
            logResult(1, "CheckWarehouseDBConnection", "Fail", "Không thể kết nối đến DB Data Warehouse: " + e.getMessage(), LocalDateTime.now(), LocalDateTime.now());
        }
        return false;
    }
    public boolean checkControlDBConnection() {
        try (Connection conn = connectToControlDB()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Kết nối tới cơ sở dữ liệu control thành công.");
                return true;
            } else {
                System.out.println("Kết nối tới cơ sở dữ liệu control thất bại.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Lỗi khi kiểm tra kết nối tới cơ sở dữ liệu control: " + e.getMessage());
            logResult(1, "CheckControlDBConnection", "Fail", "Không thể kết nối đến DB Control: " + e.getMessage(), LocalDateTime.now(), LocalDateTime.now());

            return false;
        }
    }


    // Lấy thông tin cột và lưu vào Map
    private void loadTableColumnMapping(String configId) {
        if (tableColumnMap.containsKey(configId)) return; // Nếu đã tải, bỏ qua

        List<String[]> columns = new ArrayList<>();
        String sql = "SELECT name_column, data_type FROM detail_table WHERE id_table_config = (SELECT id FROM table_configs WHERE name_table = ?)";

        try (Connection conn = connectToControlDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, configId);
            ResultSet rs = pstmt.executeQuery();

            // Kiểm tra xem có kết quả trả về không
            if (!rs.next()) {
                System.out.println("Không có metadata cho bảng: " + configId);
                return;
            }

            // Nếu có dữ liệu, thêm vào danh sách
            do {
                String nameColumn = rs.getString("name_column");
                String dataType = rs.getString("data_type");
                columns.add(new String[]{nameColumn, dataType});
            } while (rs.next());

            // Lưu vào map
            tableColumnMap.put(configId, columns);
            System.out.println("Metadata loaded for table: " + configId);
        } catch (SQLException e) {
            System.out.println("Lỗi khi tải metadata cho " + configId + ": " + e.getMessage());
        }
    }

    public String getTableName(String configId) {
        String tableName = "";
        String sql = "SELECT name_table FROM table_configs WHERE id = ?";
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



    // So sánh dữ liệu từ hai ResultSet
    private boolean compareData(ResultSet dbData, ResultSet sourceData, List<String[]> columns) throws SQLException {
        for (String[] column : columns) {
            String columnName = column[0];
            String dbValue = dbData.getString(columnName);
            String sourceValue = sourceData.getString(columnName);

            if (!Objects.equals(dbValue, sourceValue)) {
                return true; // Có sự khác biệt
            }
        }
        return false;
    }

    private String validateData(String value, String dataType, int maxLength) {
        if (value == null || value.isEmpty()) {
            return null; // Trả về null nếu giá trị rỗng
        }
        switch (dataType.toUpperCase()) {
            case "INT":
                try {
                    return String.valueOf(Integer.parseInt(value)); // Chuyển chuỗi thành số nguyên
                } catch (NumberFormatException e) {
                    return "0"; // Nếu lỗi, gán giá trị mặc định là 0
                }
            case "VARCHAR":
                if (value.length() > maxLength) {
                    return value.substring(0, maxLength); // Cắt bớt chuỗi nếu vượt quá chiều dài tối đa
                }
                return value;
            default:
                return value; // Trả về giá trị gốc nếu không cần xử lý
        }
    }

    // Chuyển dữ liệu từ staging sang warehouse
    public void migrateData(String tableName, String primaryKeyColumn) {
        loadTableColumnMapping(tableName); // Tải thông tin cột
        List<String[]> columns = tableColumnMap.get(tableName);

        if (columns == null || columns.isEmpty()) {
            System.out.println("Không có metadata cho bảng: " + tableName);
            return;
        }

        // Loại bỏ `id` khỏi danh sách cột
        List<String[]> columnsWithoutId = new ArrayList<>();
        for (String[] column : columns) {
            if (!column[0].equalsIgnoreCase("id")) {
                columnsWithoutId.add(column);
            }
        }

        // Lấy tên các cột
        String columnNames = getColumnNames(columnsWithoutId); // Chỉ lấy các cột khác `id`
        String stagingSql = "SELECT " + columnNames + " FROM staging.staging_laptop_transform";
        String warehouseSql = "SELECT * FROM datawarehouse." + tableName + " WHERE " + primaryKeyColumn + " = ?";

        // Cập nhật bản ghi cũ
        String updateSql = "UPDATE datawarehouse." + tableName +
                " SET dt_expire = CURRENT_TIMESTAMP, sku = CONCAT(sku, '_old'), is_delete = 1 WHERE " + primaryKeyColumn + " = ?";

        // Chèn bản ghi mới
        String insertSql = "INSERT INTO datawarehouse." + tableName + " (" + columnNames + ", dt_expire) VALUES (" +
                "?,".repeat(columnsWithoutId.size()) + "'9999-12-31')";

        try (Connection stagingConn = connectToStagingDB();
             Connection warehouseConn = connectToWarehouseDB();
             PreparedStatement stagingStmt = stagingConn.prepareStatement(stagingSql);
             PreparedStatement warehouseStmt = warehouseConn.prepareStatement(warehouseSql);
             PreparedStatement updateStmt = warehouseConn.prepareStatement(updateSql);
             PreparedStatement insertStmt = warehouseConn.prepareStatement(insertSql);
             ResultSet stagingRs = stagingStmt.executeQuery()) {

            while (stagingRs.next()) {
                String primaryKeyValue = stagingRs.getString(primaryKeyColumn);
                warehouseStmt.setString(1, primaryKeyValue);

                try (ResultSet warehouseRs = warehouseStmt.executeQuery()) {
                    if (warehouseRs.next()) {
                        // Bản ghi đã tồn tại -> Kiểm tra thay đổi
                        if (compareData(warehouseRs, stagingRs, columnsWithoutId)) {
                            // Cập nhật bản ghi cũ
                            updateStmt.setString(1, primaryKeyValue);
                            updateStmt.executeUpdate();

                            // Thêm bản ghi mới
                            int idx = 1;
                            for (String[] column : columnsWithoutId) {
                                insertStmt.setString(idx++, stagingRs.getString(column[0]));
                            }
                            insertStmt.executeUpdate();

                            System.out.println("Updated old record and added new record for " + primaryKeyValue);
                        } else {
                            // Không có thay đổi
                            System.out.println("No change detected for " + primaryKeyValue);
                        }
                    } else {
                        // Thêm mới nếu không tồn tại
                        int idx = 1;
                        for (String[] column : columnsWithoutId) {
                            insertStmt.setString(idx++, stagingRs.getString(column[0]));
                        }
                        insertStmt.executeUpdate();

                        System.out.println("Inserted new record for " + primaryKeyValue);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi khi migrate dữ liệu cho bảng " + tableName + ": " + e.getMessage());
        }
    }
    public void insertCurrentDateIntoDateDim() {
        // Lấy ngày hiện tại
        LocalDate currentDate = LocalDate.now();
        int dayOfWeek = currentDate.getDayOfWeek().getValue(); // Lấy giá trị ngày trong tuần (1: Monday, ..., 7: Sunday)
        int month = currentDate.getMonthValue(); // Lấy tháng
        int year = currentDate.getYear(); // Lấy năm
        String fullDate = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); // Định dạng ngày

        // SQL để chèn dữ liệu vào date_dim
        String sql = "INSERT INTO date_dim (day_of_week, full_date, month, year, created_at) VALUES (?, ?, ?, ?, ?)";

        // Kết nối tới database warehouse
        try (Connection conn = connectToWarehouseDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Thiết lập giá trị cho các cột
            pstmt.setInt(1, dayOfWeek); // day_of_week
            pstmt.setString(2, fullDate); // full_date
            pstmt.setInt(3, month); // month
            pstmt.setInt(4, year); // year
            pstmt.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now())); // created_at

            // Thực thi lệnh SQL
            int rowsInserted = pstmt.executeUpdate();
            System.out.println("Inserted " + rowsInserted + " row(s) into date_dim.");
        } catch (SQLException e) {
            System.out.println("Error inserting data into date_dim: " + e.getMessage());
        }
    }

    private void clearStagingLaptopTransform() {

        String sql = "DELETE FROM staging_laptop_transform";

        try (Connection conn = connectToStagingDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int rowsDeleted = pstmt.executeUpdate();
            System.out.println("Deleted " + rowsDeleted + " rows from staging_laptop_transform.");
        } catch (SQLException e) {
            System.out.println("Error clearing staging_laptop_transform: " + e.getMessage());
        }
    }
    private void clearStagingLaptop() {
        // Kiểm tra log mới nhất trong control
        String logCheckSql = "SELECT message, status FROM logs WHERE task_name = 'LoadFromStagingToDataWarehouse' " +
                "ORDER BY end_time DESC LIMIT 1";
        String deleteSql = "DELETE FROM staging_laptop"; // Câu lệnh xóa dữ liệu trong staging_laptop

        try (Connection controlConn = connectToControlDB();
             PreparedStatement logCheckStmt = controlConn.prepareStatement(logCheckSql);
             ResultSet logRs = logCheckStmt.executeQuery()) {

            if (logRs.next()) {
                String message = logRs.getString("message");
                String status = logRs.getString("status");

                // Kiểm tra điều kiện log
                if ("Data transferred successfully".equals(message) && "Success".equalsIgnoreCase(status)) {
                    // Thực hiện xóa dữ liệu nếu điều kiện thỏa mãn
                    try (Connection stagingConn = connectToStagingDB();
                         PreparedStatement deleteStmt = stagingConn.prepareStatement(deleteSql)) {
                        int rowsDeleted = deleteStmt.executeUpdate();
                        System.out.println("Deleted " + rowsDeleted + " rows from staging_laptop.");
                    }
                } else {
                    System.out.println("Log không thỏa mãn điều kiện, không thực hiện xóa dữ liệu trong staging_laptop.");
                }
            } else {
                System.out.println("Không tìm thấy log mới nhất để kiểm tra.");
            }

        } catch (SQLException e) {
            System.out.println("Error during log check or clearing staging_laptop: " + e.getMessage());
        }
    }



    // Phương thức hỗ trợ: Loại bỏ primaryKeyColumn khỏi danh sách columnNames
    // Chỉnh sửa lại phương thức để không loại bỏ cột nào
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
    private String getColumnNames(List<String[]> columns, String excludeColumn) {
        StringBuilder columnNames = new StringBuilder();
        for (String[] column : columns) {
            if (!column[0].equalsIgnoreCase(excludeColumn)) {
                if (columnNames.length() > 0) {
                    columnNames.append(", ");
                }
                columnNames.append(column[0]);
            }
        }
        return columnNames.toString();
    }

    private void logResult(int idConfig, String taskName, String status, String message, LocalDateTime startTime, LocalDateTime endTime) {
        int levelCode = determineLogLevel(status);  // 0 cho INFO, 2 cho ERROR
        LevelEnum level = LevelEnum.fromInt(levelCode);

        String sql = "INSERT INTO logs (id_config, task_name, status, message, end_time, created_at, level) VALUES (?, ?, ?, ?, ?, ?,?)";
        try (Connection conn = connectToControlDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idConfig);
            pstmt.setString(2, taskName);
            pstmt.setString(3, status);
            pstmt.setString(4, message);
            pstmt.setTimestamp(5, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(7, level.getLevelCode());
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
    public void sendEmail(String subject, String messageContent) {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // Tạo session cho email
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
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
    private static int determineLogLevel(String status) {
        // Mức độ log mặc định là INFO (0)
        if (status.equalsIgnoreCase("fail")) {
            return 1; // ERROR
        }
        // Nếu trạng thái là "running" hoặc bất kỳ trạng thái nào khác ngoài "fail"
        return 0; // INFO
    }

    public void executeDataWorkflow() {
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("Bắt đầu thực thi workflow...");

        try {
//      2. Kết nối database control.db
            connectToControlDB();
//      3. Kiểm tra kết nối control.db
//      4.1 Gửi mail thông báo lỗi
            if (!checkControlDBConnection()) {
                String subject = "Database Connection Error";
                String messageContent = "Failed to connect to the CONTROL database. Please check the configuration and connectivity.";

                // Gửi email báo lỗi
                sendEmail(subject, messageContent);
                System.out.println("Email thông báo lỗi đã được gửi.");
            }

//      4.2 Select dữ liệu từ logs với event Loaded
            logResult(1, "LoadFromStagingToDataWarehouse", "Running", "Loading", startTime, LocalDateTime.now());
//      5. Kiểm tra kết quả query
            if (!checkCrawlDataToStagingStatus()) {
                logResult(1, "LoadFromStagingToDataWarehouse", "Fail", "Load data was not successful recently", startTime, LocalDateTime.now());
                System.out.println("Dừng lại do tải dữ liệu không thành công gần đây.");
                return;
            }

            // Tiếp tục quy trình nếu trạng thái hợp lệ
            int idConfig = 1;
            String taskName = "LoadFromStagingToDataWarehouse";

            System.out.println("Bắt đầu chuyển đổi dữ liệu...");
//      6. Kết nối database staging.db
            connectToStagingDB();
//      7. Kiểm tra kết nối staging.db
//      8.1 Ghi log thất bại: Không có dữ liệu, Gửi mail thông báo lỗi
            if (!checkStagingDBConnection()) {
                String subject = "Database Connection Error: Staging DB";
                String messageContent = "Failed to connect to the STAGING database. Please check the configuration and connectivity.";

                // Ghi log lỗi
                logResult(0, "CheckStagingDBConnection", "Fail", "Unable to connect to STAGING database.", LocalDateTime.now(), LocalDateTime.now());

                // Gửi email thông báo lỗi
                sendEmail(subject, messageContent);
                System.out.println("Email thông báo lỗi kết nối STAGING database đã được gửi.");
            }

//      8.2 Select dữ liệu từ staging, transform dữ liệu trong staging
            transformDataToLaptopTransform();
//      9. Kết nối database warehouse.db
            connectToWarehouseDB();
//      10. Kiểm tra kết nối warehouse.db
//      11.1 Ghi log thất bại: Không có dữ liệu, Gửi mail thông báo lỗi
            if (!checkWarehouseDBConnection()) {
                String subject = "Database Connection Error: datawarehouse DB";
                String messageContent = "ail to connect to the DATAWAREHOUSE database. Please check the configuration and connectivity.";

                // Ghi log lỗi
                logResult(0, "CheckWAREHOUSEDBConnection", "Fail", "Unable to connect to DATAWAREHOUSE database.", LocalDateTime.now(), LocalDateTime.now());

                // Gửi email thông báo lỗi
                sendEmail(subject, messageContent);
                System.out.println("Email thông báo lỗi kết nối DATAWAREHOUSE database đã được gửi.");
            }
//      11.2 Hoàn tất chuyển dữ liệu
            // Di chuyển dữ liệu vào các bảng
            System.out.println("Di chuyển dữ liệu sang Brand Dim...");
            migrateData(getTableName("brand_dim_n"), "brand_id");

            System.out.println("Di chuyển dữ liệu sang Product Dim...");
            migrateData(getTableName("product_dim_n"), "sku");

            // Di chuyển dữ liệu sang Date Dim...
            System.out.println("Di chuyển dữ liệu sang Date Dim...");
//            migrateData("date_dim", "full_date");
            insertCurrentDateIntoDateDim();

//      12. Ghi log thành công
            logResult(idConfig, taskName, "Success", "Data transferred successfully", startTime, LocalDateTime.now());
            // Xóa dữ liệu trong staging_laptop_transform
            clearStagingLaptopTransform();
            clearStagingLaptop();
        } catch (Exception e) {
            System.out.println("Có lỗi xảy ra khi thực thi workflow.");

            logResult(1, "LoadFromStagingToDataWarehouse", "Fail", e.getMessage(), startTime, LocalDateTime.now());
            e.printStackTrace();
        }
        System.out.println("Kết thúc thực thi workflow.");
    }
    public static void main(String[] args) {
//        1. Load config properties
        loadConfig();
        DataMigration migration = new DataMigration();
        migration.executeDataWorkflow();
    }
}
