package com.mistrapitos.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Clase de utilidad para gestionar la conexión a la base de datos.
 */
public class DatabaseUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);
    private static HikariDataSource dataSource;
    
    /**
     * Inicializa el pool de conexiones a la base de datos.
     */
    public static void initialize() {
        try {
            Properties props = new Properties();
            props.load(DatabaseUtil.class.getResourceAsStream("/config.properties"));
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setDriverClassName("org.postgresql.Driver");
            
            // Configuración del pool de conexiones
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(30000);
            config.setMaxLifetime(1800000);
            
            // Propiedades adicionales
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(config);
            logger.info("Conexión a la base de datos inicializada correctamente");
        } catch (Exception e) {
            logger.error("Error al inicializar la conexión a la base de datos", e);
            throw new RuntimeException("Error al inicializar la conexión a la base de datos", e);
        }
    }
    
    /**
     * Obtiene una conexión del pool de conexiones.
     * @return Conexión a la base de datos
     * @throws SQLException Si ocurre un error al obtener la conexión
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initialize();
        }
        return dataSource.getConnection();
    }
    
    /**
     * Cierra el pool de conexiones.
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Conexión a la base de datos cerrada correctamente");
        }
    }
}