package com.group6.warehouse.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.boot.jdbc.DataSourceBuilder;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.group6.warehouse.datawarehouse.repository", // Repository package cho database `datawarehouse`
    entityManagerFactoryRef = "dataWarehouseEntityManagerFactory",
    transactionManagerRef = "dataWarehouseTransactionManager"
)
public class DataWarehouseDataSourceConfig {

    @Bean(name = "dataWarehouseDataSource")
    public DataSource dataWarehouseDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:mysql://localhost:3306/datawarehouse?useSSL=false&serverTimezone=UTC")
            .username("root")
            .password("123456")
            .driverClassName("com.mysql.cj.jdbc.Driver")
            .build();
    }

    @Bean(name = "dataWarehouseEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean dataWarehouseEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataWarehouseDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none"); // Tự động tạo hoặc cập nhật bảng cho database datawarehouse
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL55Dialect");

        return builder
                .dataSource(dataSource)
                .packages("com.group6.warehouse.datawarehouse.model") // Đảm bảo rằng các entity của datawarehouse nằm trong package này
                .persistenceUnit("datawarehouse")
                .properties(properties)
                .build();
    }

    @Bean(name = "dataWarehouseTransactionManager")
    public PlatformTransactionManager dataWarehouseTransactionManager(
            @Qualifier("dataWarehouseEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}
