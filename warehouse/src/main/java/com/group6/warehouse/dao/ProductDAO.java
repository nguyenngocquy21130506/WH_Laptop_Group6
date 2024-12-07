package com.group6.warehouse.dao;

import com.group6.warehouse.model.Aggregate;
import com.group6.warehouse.model.Product;
import org.springframework.beans.factory.annotation.Value;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    Connection connection;
    String urlMart = "jdbc:mysql://localhost:3306/mart";

    //    @Value("${spring.datasource.username}")
    String username = "root";
    //    @Value("${spring.datasource.password}")
    String password = "";

    public ProductDAO() {
        try {
            connect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void connect() throws SQLException {
        // connect to database
        connection = DriverManager.getConnection(urlMart, username, password);
    }

    public void disconnect() throws SQLException {
        // disconnect to database
        connection.close();
    }

    public List<Product> getAllProduct(int index) {
        List<Product> products = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM product limit 10 offset ?");
            stmt.setInt(1, (index - 1) * 10);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product product = Product.builder().
                        id(rs.getInt("id")).
                        products_name(rs.getString("product_name")).
                        price(rs.getInt("price")).
                        brand_name(rs.getString("brand_name")).
                        short_description(rs.getString("short_description")).
                        discount(rs.getInt("discount")).
                        build();
                products.add(product);
            }
            return products;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Product getProductById(int id) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM product WHERE id = ?");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Product product = Product.builder().
                        id(rs.getInt("id")).
                        products_name(rs.getString("product_name")).
                        price(rs.getInt("price")).
                        brand_name(rs.getString("brand_name")).
                        short_description(rs.getString("short_description")).
                        discount(rs.getInt("discount")).
                        discount_rate(rs.getInt("discount_rate")).
                        build();
                return product;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public List<Product> searchProduct(String name) {
        List<Product> products = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM product WHERE product_name like ?");
            stmt.setString(1, "%"+name+"%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product product = Product.builder().
                        id(rs.getInt("id")).
                        products_name(rs.getString("product_name")).
                        price(rs.getInt("price")).
                        brand_name(rs.getString("brand_name")).
                        short_description(rs.getString("short_description")).
                        discount(rs.getInt("discount")).
                        discount_rate(rs.getInt("discount_rate")).
                        build();
                products.add(product);
            }
            return products;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Aggregate getAggregate() {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM aggregate");
            ResultSet rs = stmt.executeQuery();
            int totalProduct = 0;
            int minPrice = Integer.MAX_VALUE;
            int maxPrice = Integer.MIN_VALUE;
            int avgPrice = 0;
            String minName = "";
            String maxName = "";
            while (rs.next()) {
                avgPrice = rs.getInt("avg_price_value");
                totalProduct = rs.getInt("total_products");
                minPrice = rs.getInt("min_price_value");
                maxPrice = rs.getInt("max_price_value");
                minName = rs.getString("min_price_product_name");
                maxName = rs.getString("max_price_product_name");
            }
            return Aggregate.builder().
                    id(1).
                    avg_price(avgPrice).
                    min_price(minPrice).
                    max_price(maxPrice).
                    min_price_product_name(minName).
                    max_price_product_name(maxName).
                    total_products(totalProduct + "").
                    build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
