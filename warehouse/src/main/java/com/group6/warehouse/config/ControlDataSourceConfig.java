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
    basePackages = "com.group6.warehouse.control.repository", // Repository package cho database `control`
    entityManagerFactoryRef = "controlEntityManagerFactory",
    transactionManagerRef = "controlTransactionManager"
)
public class ControlDataSourceConfig {

    @Bean(name = "controlDataSource")
    public DataSource controlDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:mysql://localhost:3306/control?useSSL=false&serverTimezone=UTC")
            .username("root")
            .password("123456")
            .driverClassName("com.mysql.cj.jdbc.Driver")
            .build();
    }

    @Bean(name = "controlEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean controlEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("controlDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none"); // Tự động tạo hoặc cập nhật bảng cho database control
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL55Dialect");

        return builder
                .dataSource(dataSource)
                .packages("com.group6.warehouse.control.model") // Đảm bảo rằng các entity của control nằm trong package này
                .persistenceUnit("control")
                .properties(properties)
                .build();
    }

    @Bean(name = "controlTransactionManager")
    public PlatformTransactionManager controlTransactionManager(
            @Qualifier("controlEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}
