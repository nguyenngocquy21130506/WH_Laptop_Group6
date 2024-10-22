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
    basePackages = "com.group6.warehouse.mart.repository", // Repository package cho database `mart`
    entityManagerFactoryRef = "martEntityManagerFactory",
    transactionManagerRef = "martTransactionManager"
)
public class MartDataSourceConfig {

    @Bean(name = "martDataSource")
    public DataSource martDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:mysql://localhost:3306/mart?useSSL=false&serverTimezone=UTC")
            .username("root")
            .password("123456")
            .driverClassName("com.mysql.cj.jdbc.Driver")
            .build();
    }

    @Bean(name = "martEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean martEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("martDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update"); // Tự động tạo hoặc cập nhật bảng cho database mart
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL55Dialect");

        return builder
                .dataSource(dataSource)
                .packages("com.group6.warehouse.mart.model") // Đảm bảo rằng các entity của mart nằm trong package này
                .persistenceUnit("mart")
                .properties(properties)
                .build();
    }

    @Bean(name = "martTransactionManager")
    public PlatformTransactionManager martTransactionManager(
            @Qualifier("martEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}
