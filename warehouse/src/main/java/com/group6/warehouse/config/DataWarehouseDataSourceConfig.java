package com.group6.warehouse.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
//    basePackages = "com.group6.warehouse.datawarehouse.repository", // Repository package cho database `datawarehouse`
    entityManagerFactoryRef = "dataWarehouseEntityManagerFactory",
    transactionManagerRef = "dataWarehouseTransactionManager"
)
public class DataWarehouseDataSourceConfig {
    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name}")
    private String driver;
    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;
    @Value("${spring.jpa.properties.hibernate.dialect}")
    private String dialect;

    @Bean(name = "dataWarehouseDataSource")
    public DataSource dataWarehouseDataSource() {
        return DataSourceBuilder.create()
            .url(url)
            .username(username)
            .password(password)
            .driverClassName(driver)
            .build();
    }

    @Bean(name = "dataWarehouseEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean dataWarehouseEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataWarehouseDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", ddlAuto); // Tự động tạo hoặc cập nhật bảng cho database datawarehouse
        properties.put("hibernate.dialect", dialect);

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
