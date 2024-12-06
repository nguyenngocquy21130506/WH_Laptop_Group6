package com.group6.warehouse;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class LoadDataToMart {

    public static void main(String[] args) {
        // Bước 1: Load các biến từ file mart.properties
        Properties properties = loadProperties("mart.properties");
        if (properties == null) {
            System.err.println("Không thể load file cấu hình mart.properties.");
            return;
        }

        String jdbcURLControl = properties.getProperty("db.control.url");
        String jdbcURLDataWarehouse = properties.getProperty("db.datawarehouse.url");
        String jdbcURLDatamart = properties.getProperty("db.mart.url");
        String username = properties.getProperty("db.username");
        String password = properties.getProperty("db.password");

        try {
            // Bước 2: Kết nối database control.db
            Connection connControl = DriverManager.getConnection(jdbcURLControl, username, password);

            // Bước 3: Kiểm tra kết nối database control
            if (connControl == null) {
                //3.1. Sendmail: Lỗi  kết nối csdl control
                new MailService().sendEmail("Load Data To Mart Failed", "Không thể kết nối đến cơ sở dữ liệu control.");
                return;
            }

            // Bước 4: Kiểm tra quá trình từ staging vào DW đã hoàn thành chưa
            if (!checkStagingProcess(connControl)) {
                //4.1. Insert 1 dòng dữ liệu vào control.log với status="Fail" and task_name="LoadDataToMart"
                insertLog(properties, "Fail", "LoadDataToMart", "Quá trình từ staging vào DW chưa hoàn thành.");
                new MailService().sendEmail("Load Data To Mart Failed", "Quá trình từ staging vào DW chưa hoàn thành.");
                return;
            }
            insertLog(properties, "Running", "LoadDataToMart", "Đang thực hiện tiến trình.");


            // Bước 5: Kết nối database datawarehouse.db
            Connection connDataWarehouse = DriverManager.getConnection(jdbcURLDataWarehouse, username, password);
            //6.Kiểm tra kết nối datawarehouse
            if (connDataWarehouse == null) {
                //6.1. Insert 1 dòng dữ liệu vào control.log với status="Fail" and task_name="LoadDataToMart"
                insertLog(properties, "Fail", "LoadDataToMart", "Lỗi kết nối cơ sở dữ liệu datawarehouse.");
                new MailService().sendEmail("Load Data To Mart Failed", "Không thể kết nối đến cơ sở dữ liệu datawarehouse.");
                return;


            }
            // Bước 7: Kết nối database datamart.db
            Connection connDatamart = DriverManager.getConnection(jdbcURLDatamart, username, password);
            //8.Kiểm tra kết nối datamart
            if (connDatamart == null) {
                //8.1. Insert 1 dòng dữ liệu vào control.log với status="fail" and event_type="LoadDataToMart"
                insertLog(properties, "Fail", "LoadDataToMart", "Lỗi kết nối cơ sở dữ liệu mart.");
                new MailService().sendEmail("Load Data To Mart Failed", "Không thể kết nối đến cơ sở dữ liệu mart.");
                return;
            }

            // Bước 9: Sao chép dữ liệu từ datawarehouse vào bảng tạm mart
            copyDataToMart(connDataWarehouse, connDatamart);

            // Bước 10: Đổi tên bảng từ tạm thành chính và tạo  lại bảng tạm
            renameTables(connDatamart);

            // Bước 11: Xóa bảng backup
            dropBackupTables(connDatamart);

            // Bước 12: Ghi log thành công
            insertLog(properties, "Success", "LoadDataToMart", "Dữ liệu đã được load thành công vào mart.");
            new MailService().sendEmail("Load Data To Mart Success", "Dữ liệu đã được load thành công vào mart.");

            // Bước 13: Đóng kết nối
            closeConnections(connControl, connDataWarehouse, connDatamart);

        } catch (SQLException e) {
            System.err.println("Lỗi trong quá trình kết nối hoặc thao tác dữ liệu: " + e.getMessage());
        }
    }


    private static Properties loadProperties(String fileName) {
        Properties properties = new Properties();
        try (InputStream input = LoadDataToMart.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                System.err.println("Không tìm thấy file cấu hình.");
                return null;
            }
            properties.load(input);
        } catch (Exception e) {
            System.err.println("Lỗi khi load file cấu hình: " + e.getMessage());
            return null;
        }
        return properties;
    }



    // Kiểm tra quá trình từ staging vào DW
    private static boolean checkStagingProcess(Connection connControl) throws SQLException {
        String checkLogSQL = "SELECT CASE WHEN status = 'Success' THEN 1 ELSE 0 END AS is_success "
                + "FROM logs WHERE task_name = 'LoadFromStagingToDatawarehouse' "
                + "AND DATE(created_at) = CURDATE() ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connControl.prepareStatement(checkLogSQL)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("is_success") != 1) {
                    return false;
                }
            }
        }
        return true;
    }

    // Sao chép dữ liệu vào mart từ datawarehouse
    private static void copyDataToMart(Connection connDataWarehouse, Connection connDatamart) throws SQLException {
        // Bước 5.3: Sao chép dữ liệu từ date_dim vào date_temp
        String copyDateSQL = "INSERT INTO mart.date_temp (day_of_week, full_date, month, year) "
                + "SELECT day_of_week, full_date, month, year FROM datawarehouse.date_dim;";
        try (Statement stmt = connDatamart.createStatement()) {
            stmt.executeUpdate(copyDateSQL);  // Sao chép dữ liệu vào bảng date_temp
        }

        //Sao chép dữ liệu từ product_dim vào product_temp với việc lấy brand_name từ brand_dim
        String copyProductSQL = "INSERT INTO mart.product_temp (id, brand_id, brand_name, discount, discount_rate, inventory_status, price, product_name, review_count, short_description, sku, stock_item_qty) "
                + "SELECT p.id, p.brand_id, b.brand_name, p.discount, p.discount_rate, p.inventory_status, p.price, p.product_name, p.review_count, p.short_description, p.sku, p.stock_item_qty "
                + "FROM datawarehouse.product_dim p "
                + "JOIN datawarehouse.brand_dim b ON p.brand_id = b.brand_id;";  // JOIN bảng brand_dim để lấy brand_name
        try (Statement stmt = connDatamart.createStatement()) {
            stmt.executeUpdate(copyProductSQL);  // Sao chép dữ liệu vào bảng product_temp
        }

        //Sao chép dữ liệu từ brand_dim vào brand_temp
        String copyBrandSQL = "INSERT INTO mart.brand_temp (id, brand_name, brand_id) "
                + "SELECT b.id, b.brand_name, b.brand_id "
                + "FROM datawarehouse.brand_dim b;";
        try (Statement stmt = connDatamart.createStatement()) {
            stmt.executeUpdate(copyBrandSQL);  // Sao chép dữ liệu vào bảng brand_temp
        }
        String copyPriceAggregateSQL = "INSERT INTO mart.aggregate_temp (sk, avg_price_value, min_price_value, max_price_value, total_price_value,  min_price_product_name, max_price_product_name, total_products) "
                + "SELECT sk, avg_price_value, min_price_value, max_price_value, total_price_value,  min_price_product_name, max_price_product_name, total_products "
                + "FROM datawarehouse.price_aggregate "
                + "WHERE sk = CONCAT('aggregate_summary_', CURDATE()) "
                + "LIMIT 1;";  // Lấy dữ liệu mới nhất theo ngày hiện tại
        try (Statement stmt = connDatamart.createStatement()) {
            stmt.executeUpdate(copyPriceAggregateSQL);
        }
    }


    //Đổi tên bảng từ tạm thành chính và tạo lại bảng tạm
    private static void renameTables(Connection connDatamart) throws SQLException {
        // Đổi tên các bảng tạm thành bảng chính
        String renameDateSQL = "RENAME TABLE mart.date TO mart.date_backup, mart.date_temp TO mart.date;";
        String renameProductSQL = "RENAME TABLE mart.product TO mart.product_backup, mart.product_temp TO mart.product;";
        String renameBrandSQL = "RENAME TABLE mart.brand TO mart.brand_backup, mart.brand_temp TO mart.brand;";
        String renameAggregateSQL = "RENAME TABLE mart.aggregate TO mart.aggregate_backup, mart.aggregate_temp TO mart.aggregate;";


        try (Statement stmt = connDatamart.createStatement()) {
            stmt.executeUpdate(renameDateSQL);  // Đổi tên bảng 'date' thành 'date_backup' và 'date_temp' thành 'date'
            stmt.executeUpdate(renameProductSQL);  // Đổi tên bảng 'product' thành 'product_backup' và 'product_temp' thành 'product'
            stmt.executeUpdate(renameBrandSQL);  // Đổi tên bảng 'brand' thành 'brand_backup' và 'brand_temp' thành 'brand'
            stmt.executeUpdate(renameAggregateSQL);

            // Tạo lại bảng tạm cho lần chạy sau
            String createDateTempSQL = "CREATE TABLE mart.date_temp LIKE mart.date;";
            String createProductTempSQL = "CREATE TABLE mart.product_temp LIKE mart.product;";
            String createBrandTempSQL = "CREATE TABLE mart.brand_temp LIKE mart.brand;";
            String createAggregateTempSQL = "CREATE TABLE mart.aggregate_temp LIKE mart.aggregate;";

            stmt.executeUpdate(createDateTempSQL);  // Tạo lại bảng 'date_temp'
            stmt.executeUpdate(createProductTempSQL);  // Tạo lại bảng 'product_temp'
            stmt.executeUpdate(createBrandTempSQL);  // Tạo lại bảng 'brand_temp'
            stmt.executeUpdate(createAggregateTempSQL);
        }
    }


    // Xóa các bảng backup
    private static void dropBackupTables(Connection connDatamart) throws SQLException {
        // Lệnh SQL để xóa các bảng backup
        String dropDateBackupSQL = "DROP TABLE IF EXISTS mart.date_backup;";
        String dropProductBackupSQL = "DROP TABLE IF EXISTS mart.product_backup;";
        String dropBrandBackupSQL = "DROP TABLE IF EXISTS mart.brand_backup;";
        String dropAggregateBackupSQL = "DROP TABLE IF EXISTS mart.aggregate_backup;";

        try (Statement stmt = connDatamart.createStatement()) {
            stmt.executeUpdate(dropDateBackupSQL);  // Xóa bảng 'date_backup'
            stmt.executeUpdate(dropProductBackupSQL);  // Xóa bảng 'product_backup'
            stmt.executeUpdate(dropBrandBackupSQL);  // Xóa bảng 'brand_backup'
            stmt.executeUpdate(dropAggregateBackupSQL);
        }
    }

    //Insert log vào bảng control.log
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

    // Đóng kết nối
    private static void closeConnections(Connection connControl, Connection connDataWarehouse, Connection connDatamart) {
        try {
            if (connControl != null) connControl.close();
            if (connDataWarehouse != null) connDataWarehouse.close();
            if (connDatamart != null) connDatamart.close();
        } catch (SQLException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
}
