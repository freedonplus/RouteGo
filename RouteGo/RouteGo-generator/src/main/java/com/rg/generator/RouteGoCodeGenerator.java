package com.rg.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RouteGoCodeGenerator {

    private static final Map<String, String> SERVICES = serviceMappings();

    private RouteGoCodeGenerator() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            generateAllServices();
            return;
        }
        if (args.length < 2) {
            printUsage();
            return;
        }

        String serviceName = args[0].trim().toLowerCase();
        String database = SERVICES.get(serviceName);
        if (database == null) {
            printUsage();
            return;
        }

        String[] tables = Arrays.copyOfRange(args, 1, args.length);
        generate(serviceName, database, tables);
    }

    private static void generateAllServices() {
        String host = env("MYSQL_HOST", "127.0.0.1");
        String port = env("MYSQL_PORT", "3306");
        String username = env("MYSQL_USERNAME", "root");
        String password = env("MYSQL_PASSWORD", "1234");

        for (Map.Entry<String, String> entry : SERVICES.entrySet()) {
            String serviceName = entry.getKey();
            String database = entry.getValue();
            String url = buildUrl(host, port, database);
            String[] tables = listTables(url, username, password, database);
            if (tables.length == 0) {
                System.out.println("No business tables found for " + database + ", skipped.");
                continue;
            }
            System.out.println("Generating " + serviceName + " -> " + Arrays.toString(tables));
            generate(serviceName, database, tables);
        }
    }

    private static void generate(String serviceName, String database, String[] tables) {
        Path projectRoot = resolveProjectRoot();
        Path javaOutput = projectRoot.resolve("RouteGo-modules")
                .resolve("RouteGo-" + serviceName)
                .resolve("src/main/java");
        Path xmlOutput = projectRoot.resolve("RouteGo-modules")
                .resolve("RouteGo-" + serviceName)
                .resolve("src/main/resources/mapper")
                .resolve(serviceName);
        String host = env("MYSQL_HOST", "127.0.0.1");
        String port = env("MYSQL_PORT", "3306");
        String username = env("MYSQL_USERNAME", "root");
        String password = env("MYSQL_PASSWORD", "1234");
        String url = buildUrl(host, port, database);

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                        .author("RouteGo")
                        .outputDir(javaOutput.toString())
                        .disableOpenDir()
                        .dateType(DateType.TIME_PACK)
                        .commentDate("yyyy-MM-dd"))
                .packageConfig(builder -> builder
                        .parent("com.rg")
                        .moduleName(serviceName)
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .pathInfo(Map.of(OutputFile.xml, xmlOutput.toString())))
                .strategyConfig(builder -> builder
                        .addInclude(tables)
                        .entityBuilder()
                        .enableFileOverride()
                        .enableTableFieldAnnotation()
                        .mapperBuilder()
                        .enableBaseResultMap()
                        .enableBaseColumnList()
                        .serviceBuilder()
                        .formatServiceFileName("%sService")
                        .controllerBuilder()
                        .enableRestStyle())
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    private static String buildUrl(String host, String port, String database) {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                + "&useSSL=false&allowPublicKeyRetrieval=true";
    }

    private static String[] listTables(String url, String username, String password, String database) {
        String sql = "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name <> 'undo_log' ORDER BY table_name";
        List<String> names = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read tables from " + database, e);
        }
        return names.toArray(new String[0]);
    }

    private static Map<String, String> serviceMappings() {
        Map<String, String> services = new LinkedHashMap<>();
        services.put("admin", "routego_admin");
        services.put("catalog", "routego_catalog");
        services.put("trade", "routego_trade");
        services.put("customer", "routego_customer");
        services.put("operations", "routego_operations");
        return services;
    }

    private static Path resolveProjectRoot() {
        Path configured = Path.of(env("ROUTEGO_PROJECT_ROOT", System.getProperty("user.dir")));
        Path current = configured.toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("RouteGo-modules"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isDirectory(parent.resolve("RouteGo-modules"))) {
            return parent;
        }
        throw new IllegalStateException("Could not resolve RouteGo project root: " + current);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static void printUsage() {
        System.err.println("No args: generates all RouteGo services automatically.");
        System.err.println("Usage: RouteGoCodeGenerator <admin|catalog|trade|customer|operations> <table> [table...]");
        System.err.println("Set MYSQL_HOST, MYSQL_PORT, MYSQL_USERNAME and MYSQL_PASSWORD before running.");
    }
}
