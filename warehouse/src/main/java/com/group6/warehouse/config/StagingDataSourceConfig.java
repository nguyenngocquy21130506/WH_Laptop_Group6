package com.group6.warehouse.config;


import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.group6.warehouse.staging.repository", 
    entityManagerFactoryRef = "stagingEntityManagerFactory", 
    transactionManagerRef = "stagingTransactionManager"
)
public class StagingDataSourceConfig {

    @Bean(name = "stagingDataSource")
    @Primary
    public DataSource stagingDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:mysql://localhost:3306/staging?useSSL=false&serverTimezone=UTC")
            .username("root")
            .password("123456")
            .driverClassName("com.mysql.cj.jdbc.Driver")
            .build();
    }

    @Bean(name = "stagingEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean stagingEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("stagingDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL55Dialect");

        return builder
                .dataSource(dataSource)
                .packages("com.group6.warehouse.staging.model") // Đảm bảo đường dẫn tới các entity của staging
                .persistenceUnit("staging")
                .properties(properties)
                .build();
    }

    @Bean(name = "stagingTransactionManager")
    @Primary
    public PlatformTransactionManager stagingTransactionManager(
            @Qualifier("stagingEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}
