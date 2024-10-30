package com.group6.warehouse.config;


import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
//    basePackages = "com.group6.warehouse.staging.repository",
    entityManagerFactoryRef = "stagingEntityManagerFactory", 
    transactionManagerRef = "stagingTransactionManager"
)
public class StagingDataSourceConfig {
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

    @Bean(name = "stagingDataSource")
    @Primary
    public DataSource stagingDataSource() {
        return DataSourceBuilder.create()
            .url(url)
            .username(username)
            .password(password)
            .driverClassName(driver)
            .build();
    }

    @Bean(name = "stagingEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean stagingEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("stagingDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", ddlAuto);
        properties.put("hibernate.dialect", dialect);

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
