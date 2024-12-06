package com.group6.warehouse;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class TransformAggregate {

    public static void main(String[] args) throws SQLException, IOException {
        // Kiểm tra tham số dòng lệnh
        if (args.length > 1) {
            return;
        }

        // Lấy ngày chạy từ tham số dòng lệnh (nếu có), nếu không thì dùng ngày hiện tại
        String dateInput = (args.length == 1) ? args[0] : ""; // Ngày chạy là tham số đầu tiên (nếu có)

        Date runDate = new Date(); // Mặc định là ngày hiện tại
        if (!dateInput.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                runDate = sdf.parse(dateInput);
            } catch (Exception e) {
                System.err.println("Ngày không hợp lệ. Sử dụng ngày hiện tại.");
                runDate = new Date();
            }
        }

        Properties properties = new Properties();

        // 1. Load các biến cấu hình từ file properties
        InputStream input = TransformAggregate.class.getClassLoader().getResourceAsStream("config.properties");
        properties.load(input);

        // 2. Kết nối database control.db
        String jdbcURLControl = properties.getProperty("db.control.url");
        String jdbcURLDataWarehouse = properties.getProperty("db.datawarehouse.url");
        String username = properties.getProperty("db.username");
        String password = properties.getProperty("db.password");
        // 3. Kiểm tra kết nối database control
        try (Connection connControl = DriverManager.getConnection(jdbcURLControl, username, password)) {
            System.out.println("Đã kết nối đến cơ sở dữ liệu control.");
            insertLog(properties, "Running", "Transform Aggregate", "Đang thực hiện tiến trình.");

            // 4. Kiểm tra quá trình từ staging vào DW đã run xong chưa

            String checkLogSQL = "SELECT CASE WHEN status = 'Success' THEN 1 ELSE 0 END AS is_success "
                    + "FROM logs WHERE task_name = 'LoadFromStagingToDatawarehouse' "
                    + "AND DATE(created_at) = ? ORDER BY created_at DESC LIMIT 1";

            try (PreparedStatement ps = connControl.prepareStatement(checkLogSQL)) {
                // Đặt giá trị tham số cho ngày chạy (runDate)
                ps.setDate(1, new java.sql.Date(runDate.getTime())); // Chuyển runDate thành java.sql.Date

                try (ResultSet resultSet = ps.executeQuery()) {
                    // Format ngày chạy (runDate) để hiển thị trong thông báo
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    String formattedDate = sdf.format(runDate);

                    if (!resultSet.next()) {
                        String message = "Quá trình từ staging vào DW chưa chạy trong ngày " + formattedDate + ".";
                        System.out.println(message);
                        insertLog(properties, "Fail", "Transform Aggregate", message);
                        new MailService().sendEmail("Transform Aggregate Failed", message);
                        return;
                    }
                    if (resultSet.getInt("is_success") != 1) {
                        String message = "Quá trình từ staging vào DW chưa hoàn thành trong ngày " + formattedDate + ".";
                        System.out.println(message);
                        insertLog(properties, "Fail", "Transform Aggregate", message);
                        new MailService().sendEmail("Transform Aggregate Failed", message);
                        return;
                    }
                }
            }


            // 5. Kết nối đến datawarehouse.db
            try (Connection connDataWarehouse = DriverManager.getConnection(jdbcURLDataWarehouse, username, password)) {
                System.out.println("Đã kết nối đến cơ sở dữ liệu datawarehouse.");

                // 6. Lấy thông tin bảng từ table_config để thực hiện các tính toán tổng hợp
                String getProductTableConfig = "SELECT name_table, name_db FROM table_configs WHERE name_table = 'product_dim' AND name_db = 'datawarehouse'";
                String getBrandTableConfig = "SELECT name_table, name_db FROM table_configs WHERE name_table = 'brand_dim' AND name_db = 'datawarehouse'";

                String productTableName = null;
                String brandTableName = null;

                try (Statement statement = connControl.createStatement()) {
                    ResultSet rs = statement.executeQuery(getProductTableConfig);
                    if (rs.next()) {
                        productTableName = rs.getString("name_table");
                    }

                    rs = statement.executeQuery(getBrandTableConfig);
                    if (rs.next()) {
                        brandTableName = rs.getString("name_table");
                    }
                }

                if (productTableName == null || brandTableName == null) {
                    System.err.println("Không tìm thấy thông tin bảng cần thiết trong table_config.");
                    insertLog(properties, "Fail", "Transform Aggregate", "Không tìm thấy thông tin bảng cần thiết trong table_config.");
                    new MailService().sendEmail("Transform Aggregate Failed", "Không tìm thấy thông tin bảng cần thiết trong table_config.");
                    return;
                }

                // 7. Insert vào bảng price_aggregate và brand_aggregate
                String priceAggregateSQL = String.format(
                        "INSERT INTO price_aggregate (sk, avg_price_value, min_price_value, max_price_value, total_price_value, avg_discount_value, min_price_product_name, max_price_product_name, avg_price_product_name, total_products, create_at) "
                                + "SELECT CONCAT('aggregate_summary_', CURDATE()) AS sk, "
                                + "(SELECT AVG(price) FROM %s) AS avg_price_value, "
                                + "(SELECT MIN(price) FROM %s) AS min_price_value, "
                                + "(SELECT MAX(price) FROM %s) AS max_price_value, "
                                + "(SELECT SUM(price) FROM %s) AS total_price_value, "
                                + "AVG(p.discount_rate) AS avg_discount_value, "
                                + "(SELECT product_name FROM %s WHERE price = (SELECT MIN(price) FROM %s) LIMIT 1) AS min_price_product_name, "
                                + "(SELECT product_name FROM %s WHERE price = (SELECT MAX(price) FROM %s) LIMIT 1) AS max_price_product_name, "
                                + "(SELECT product_name FROM %s WHERE ABS(price - (SELECT AVG(price) FROM %s)) = "
                                + "(SELECT MIN(ABS(price - (SELECT AVG(price) FROM %s))) FROM %s) LIMIT 1) AS avg_price_product_name, "
                                + "COUNT(p.id) AS total_products, NOW() "
                                + "FROM %s p "
                                + "GROUP BY sk "
                                + "ON DUPLICATE KEY UPDATE "
                                + "avg_price_value = VALUES(avg_price_value), min_price_value = VALUES(min_price_value), "
                                + "max_price_value = VALUES(max_price_value), total_price_value = VALUES(total_price_value), "
                                + "avg_discount_value = VALUES(avg_discount_value), min_price_product_name = VALUES(min_price_product_name), "
                                + "max_price_product_name = VALUES(max_price_product_name), avg_price_product_name = VALUES(avg_price_product_name), "
                                + "total_products = VALUES(total_products), create_at = NOW()",
                        productTableName, productTableName, productTableName, productTableName,
                        productTableName, productTableName, productTableName, productTableName,
                        productTableName, productTableName, productTableName, productTableName, productTableName);

                String brandAggregateSQL = String.format(
                        "INSERT INTO brand_aggregate (sk, brand_name, total_models, total_reviews, avg_rating, stock_available, create_at) "
                                + "SELECT CONCAT(b.brand_name, '_', CURDATE()) AS sk, b.brand_name, "
                                + "COUNT(DISTINCT p.id) AS total_models, "
                                + "SUM(p.review_count) AS total_reviews, AVG(p.discount_rate) AS avg_rating, "
                                + "SUM(p.stock_item_qty) AS stock_available, NOW() "
                                + "FROM %s b JOIN %s p ON p.brand_id = b.brand_id "
                                + "GROUP BY b.brand_name "
                                + "ON DUPLICATE KEY UPDATE "
                                + "total_models = VALUES(total_models), total_reviews = VALUES(total_reviews), "
                                + "avg_rating = VALUES(avg_rating), stock_available = VALUES(stock_available), "
                                + "create_at = NOW()",
                        brandTableName, productTableName);

                try (Statement statement = connDataWarehouse.createStatement()) {
                    statement.executeUpdate(priceAggregateSQL);
                    System.out.println("Dữ liệu đã được chèn hoặc cập nhật trong bảng price_aggregate.");
                    statement.executeUpdate(brandAggregateSQL);
                    System.out.println("Dữ liệu đã được chèn hoặc cập nhật trong bảng brand_aggregate.");
                }

                // 8. Insert 1 dòng dữ liệu vào control.log với status="Success"
                insertLog(properties, "Success", "Transform Aggregate", "Tổng hợp dữ liệu thành công.");
                new MailService().sendEmail("Transform Aggregate Success", "Tổng hợp dữ liệu thành công.");

            } catch (SQLException e) {
                System.err.println("Lỗi kết nối cơ sở dữ liệu datawarehouse: " + e.getMessage());
                insertLog(properties, "Fail", "Transform Aggregate", "Lỗi kết nối cơ sở dữ liệu datawarehouse: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối cơ sở dữ liệu control: " + e.getMessage());
            insertLog(properties, "Fail", "Transform Aggregate", "Lỗi kết nối cơ sở dữ liệu control: " + e.getMessage());
        }
    }

    // Phương thức insertLog
    private static void insertLog(Properties properties, String status, String taskName, String message) {
        String jdbcURLControl = properties.getProperty("db.control.url");
        String username = properties.getProperty("db.username");
        String password = properties.getProperty("db.password");

        // Xác định mức độ log từ status
        int levelCode = determineLogLevel(status);  // 0 cho INFO, 2 cho ERROR
        LevelEnum level = LevelEnum.fromInt(levelCode);

        // Câu lệnh SQL INSERT vào bảng logs
        String insertLogSQL = "INSERT INTO logs (id_config, status, task_name, message, created_at, end_time, level) "
                + "VALUES (?, ?, ?, ?, NOW(), NOW(), ?)";

        try (Connection conn = DriverManager.getConnection(jdbcURLControl, username, password);
             PreparedStatement ps = conn.prepareStatement(insertLogSQL)) {

            // Set các giá trị cho PreparedStatement
            ps.setLong(1, 1);  // id_config mặc định là 1
            ps.setString(2, status);
            ps.setString(3, taskName);
            ps.setString(4, message);
            ps.setInt(5, level.getLevelCode());  // Mức độ log (0 - INFO, 2 - ERROR)

            // Thực thi câu lệnh INSERT
            ps.executeUpdate();
            System.out.println("Log đã được chèn vào bảng logs.");
        } catch (SQLException e) {
            System.err.println("Lỗi khi ghi log: " + e.getMessage());
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

}
