import java.sql.*;
import java.time.LocalDateTime;

public class DataMigration {

    // Thông tin kết nối cơ sở dữ liệu
    private static final String CONTROL_DB_URL = "jdbc:mysql://localhost:3306/control";
    private static final String WAREHOUSE_DB_URL = "jdbc:mysql://localhost:3306/datawarehouse";
    private static final String STAGING_DB_URL = "jdbc:mysql://localhost:3306/staging";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    // Kết nối tới cơ sở dữ liệu control_db
    private Connection connectToControlDB() throws SQLException {
        return DriverManager.getConnection(CONTROL_DB_URL, USER, PASSWORD);
    }

    // Kết nối tới cơ sở dữ liệu warehouse_db
    private Connection connectToWarehouseDB() throws SQLException {
        return DriverManager.getConnection(WAREHOUSE_DB_URL, USER, PASSWORD);
    }

    // Kết nối tới cơ sở dữ liệu staging_db
    private Connection connectToStagingDB() throws SQLException {
        return DriverManager.getConnection(STAGING_DB_URL, USER, PASSWORD);
    }

    // Chuyển dữ liệu từ staging sang product_dim
    private void migrateToProductDim() {
        String insertSql = "INSERT INTO product_dim (sku, product_name, short_description, price, discount, discount_rate, review_count, inventory_status, stock_item_qty) " +
                "SELECT sku, product_name, short_description, CAST(price AS UNSIGNED), CAST(discount AS UNSIGNED), " +
                "CAST(discount_rate AS UNSIGNED), CAST(review_count AS UNSIGNED), inventory_status, CAST(stock_item_qty AS UNSIGNED) " +
                "FROM staging.staging_laptop s " +
                "WHERE NOT EXISTS (SELECT 1 FROM datawarehouse.product_dim p WHERE p.sku = s.sku)";
        executeInsert(insertSql, "product_dim");
    }

    // Chuyển dữ liệu từ staging sang date_dim
    private void migrateToDateDim() {
        String insertSql = "INSERT INTO date_dim (full_date) " +
                "SELECT DISTINCT created_at FROM staging.staging_laptop s " +
                "WHERE NOT EXISTS (SELECT 1 FROM datawarehouse.date_dim d WHERE d.full_date = s.created_at)";
        executeInsert(insertSql, "date_dim");
    }

    // Chuyển dữ liệu từ staging sang brand_dim
    private void migrateToBrandDim() {
        String insertSql = "INSERT INTO brand_dim (id, brand_name) " +
                "SELECT DISTINCT CAST(brand_id AS INT), brand_name FROM staging.staging_laptop s " +
                "WHERE NOT EXISTS (SELECT 1 FROM datawarehouse.brand_dim b WHERE b.brand_name = s.brand_name)";
        executeInsert(insertSql, "brand_dim");
    }

    // Thực thi truy vấn insert và hiển thị kết quả
    private void executeInsert(String sql, String tableName) {
        try (Connection conn = connectToWarehouseDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int rowsInserted = pstmt.executeUpdate();
            System.out.println("Rows inserted into " + tableName + ": " + rowsInserted);

        } catch (SQLException e) {
            System.out.println("Error inserting data into " + tableName + ": " + e.getMessage());
        }
    }

    // Xóa dữ liệu trong staging_table sau khi chuyển đổi
    private void clearStagingTable() {
        String deleteSql = "DELETE FROM staging.staging_laptop";
        try (Connection conn = connectToStagingDB();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {

            int rowsDeleted = pstmt.executeUpdate();
            System.out.println("Rows deleted from staging_laptop: " + rowsDeleted);

        } catch (SQLException e) {
            System.out.println("Error deleting data from staging_laptop: " + e.getMessage());
        }
    }

    // Kiểm tra logs trong control DB
    private boolean checkControlLog() {
        String sql = "SELECT * FROM logs WHERE status = 'SUCCESS'";
        try (Connection conn = connectToControlDB();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            return rs.next(); // Trả về true nếu có dữ liệu

        } catch (SQLException e) {
            System.out.println("Error checking control log: " + e.getMessage());
            return false;
        }
    }

    // Ghi log kết quả vào control DB
    private void logResult(int idConfig, String taskName, String status, String message, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "INSERT INTO logs (id_config, task_name, status, message, end_time, created_at) VALUES ( ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectToControlDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idConfig);
            pstmt.setString(2, taskName);
            pstmt.setString(3, status);
            pstmt.setString(4, message);
            pstmt.setTimestamp(5, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now())); // Ghi thời gian hiện tại vào created_at

            pstmt.executeUpdate();
            System.out.println("Log entry successfully added to logs table.");

        } catch (SQLException e) {
            System.out.println("Error logging result: " + e.getMessage());
        }
    }

    // Phương thức chính để thực thi quá trình chuyển dữ liệu
    public void executeDataWorkflow() {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            // Kiểm tra log trong control_db trước khi thực hiện
            if (!checkControlLog()) {
                logResult(1, "LoadFromStagingToDatawarehouse", "FAILED", "No data in control log", startTime, LocalDateTime.now());
                return;
            }

            int idConfig = 1; // ID cấu hình
            String taskName = "LoadFromStagingToDatawarehouse"; // Tên tác vụ

            // Thực hiện chuyển đổi dữ liệu
            migrateToProductDim();
            migrateToDateDim();
            migrateToBrandDim();

            // Xóa dữ liệu trong staging sau khi hoàn tất chuyển đổi
//            clearStagingTable();

            // Ghi lại log khi thành công
            logResult(idConfig, taskName, "SUCCESS", "Data transferred successfully", startTime, LocalDateTime.now());

        } catch (Exception e) {
            // Ghi lại log khi xảy ra lỗi
            logResult(1, "LoadFromStagingToDatawarehouse", "FAILED", e.getMessage(), startTime, LocalDateTime.now());
        }
    }

    // Hàm main để chạy ứng dụng
    public static void main(String[] args) {
        DataMigration migration = new DataMigration();
        migration.executeDataWorkflow();
    }
}
