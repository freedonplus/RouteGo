package com.rg.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RouteGo 单体多模块应用唯一启动入口。
 * <p>
 * 业务模块仍然是独立的 Maven 模块（admin/catalog/trade/customer/operations），
 * 但运行在同一个 Spring 容器中，通过本地方法调用协作，不再有网关、注册中心和 Feign。
 */
@SpringBootApplication(scanBasePackages = "com.rg")
@MapperScan({
        "com.rg.admin.mapper",
        "com.rg.catalog.mapper",
        "com.rg.trade.mapper",
        "com.rg.customer.mapper",
        "com.rg.operations.mapper"
})
@EnableAsync
@EnableScheduling
public class RouteGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RouteGoApplication.class, args);
    }
}
