//package com.group6.warehouse.scheme;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import static org.hibernate.tool.schema.SchemaToolingLogging.LOGGER;
//
//@Component
//public class ScheduledTasks {
//    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
//
////    @Scheduled(fixedDelay = 10000)
////    public void scheduleTaskWithFixedDelay() {
////        // Call send email method here
////        // Pretend it takes 1000ms
////        try {
////            Thread.sleep(1000);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
////        LOGGER.info("Send email to producers to inform quantity sold items");
////    }
////
////    @Scheduled(fixedRate = 2000, initialDelay = 10000)
////    public void scheduleTaskWithInitialDelay() {
////        LOGGER.info("Send email to producers to inform quantity sold items");
////    }
//
//    @Scheduled(cron = "0 54 14 * * 1,2,3,5")
//    public void scheduleTaskWithCronExpression() {
//        LOGGER.info("Send email to producers to inform quantity sold items");
//    }
//}
//
