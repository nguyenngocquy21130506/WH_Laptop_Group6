import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;


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

    private final Map<String, List<String[]>> tableColumnMap = new HashMap<>();

    // Tải cấu hình từ file config.properties
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

    private Connection connectToControlDB() throws SQLException {
        return DriverManager.getConnection(CONTROL_DB_URL, USER, PASSWORD);
    }

    private Connection connectToWarehouseDB() throws SQLException {
        return DriverManager.getConnection(WAREHOUSE_DB_URL, USER, PASSWORD);
    }

    private Connection connectToStagingDB() throws SQLException {
        return DriverManager.getConnection(STAGING_DB_URL, USER, PASSWORD);
    }

    // Lấy thông tin cột và lưu vào Map
    private void loadTableColumnMapping(String configId) {
        if (tableColumnMap.containsKey(configId)) return; // Nếu đã tải, bỏ qua

        List<String[]> columns = new ArrayList<>();
        String sql = "SELECT name_column, data_type FROM detail_table WHERE id_table_config = (SELECT id FROM table_config WHERE name_table = ?)";

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

        // Loại bỏ cột `id` khỏi danh sách cột
        List<String[]> columnsWithoutId = new ArrayList<>();
        for (String[] column : columns) {
            if (!column[0].equalsIgnoreCase("id")) {
                columnsWithoutId.add(column);
            }
        }

        String columnNames = getColumnNames(columnsWithoutId); // Lấy danh sách cột, bỏ qua `id`
        String stagingSql = "SELECT " + columnNames + " FROM staging.staging_laptop_transform";
        String warehouseSql = "SELECT * FROM datawarehouse." + tableName + " WHERE " + primaryKeyColumn + " = ?";

        // Cập nhật bản ghi cũ
        String updateSql = "UPDATE datawarehouse." + tableName +
                " SET dt_expire = CURRENT_TIMESTAMP, sku = CONCAT(sku, '_old'), is_delete = 1 WHERE " + primaryKeyColumn + " = ?";

        // Chèn bản ghi mới (bỏ qua `id`)
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
                        // So sánh dữ liệu
                        if (compareData(warehouseRs, stagingRs, columnsWithoutId)) {
                            // Cập nhật bản ghi cũ
                            updateStmt.setString(1, primaryKeyValue);
                            updateStmt.executeUpdate();

                            // Chèn bản ghi mới
                            int idx = 1;
                            for (String[] column : columnsWithoutId) {
                                insertStmt.setString(idx++, stagingRs.getString(column[0]));
                            }
                            insertStmt.executeUpdate();

                            System.out.println("Updated old record and added new record for " + primaryKeyValue);
                        } else {
                            System.out.println("No change detected for " + primaryKeyValue);
                        }
                    } else {
                        // Thêm mới nếu không có bản ghi trong warehouse
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

    public void executeDataWorkflow() {
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("Bắt đầu thực thi workflow...");

        try {
            // Kiểm tra trạng thái của crawl data
            if (!checkCrawlDataToStagingStatus()) {
                logResult(1, "Load", "FAILED", "Load data was not successful recently", startTime, LocalDateTime.now());
                System.out.println("Dừng lại do tải dữ liệu không thành công gần đây.");
                return;
            }

            // Tiếp tục quy trình nếu trạng thái hợp lệ
            int idConfig = 1;
            String taskName = "LoadFromStagingToWarehouse";

            System.out.println("Bắt đầu chuyển đổi dữ liệu...");
            transformDataToLaptopTransform();

            // Di chuyển dữ liệu vào các bảng
            System.out.println("Di chuyển dữ liệu sang Brand Dim...");
            migrateData("brand_dim", "brand_id");

            System.out.println("Di chuyển dữ liệu sang Product Dim...");
            migrateData("product_dim", "sku");

            // Di chuyển dữ liệu sang Date Dim...
//            System.out.println("Di chuyển dữ liệu sang Date Dim...");
//            migrateData("date_dim", "full_date");

            // Ghi log kết quả thành công
            logResult(idConfig, taskName, "SUCCESS", "Data transferred successfully", startTime, LocalDateTime.now());

        } catch (Exception e) {
            System.out.println("Có lỗi xảy ra khi thực thi workflow.");
            logResult(1, "LoadFromStagingToWarehouse", "FAILED", e.getMessage(), startTime, LocalDateTime.now());
            e.printStackTrace();
        }
        System.out.println("Kết thúc thực thi workflow.");
    }


    public static void main(String[] args) {
        loadConfig();
        Backup1 migration = new Backup1();
        migration.executeDataWorkflow();
    }
}
