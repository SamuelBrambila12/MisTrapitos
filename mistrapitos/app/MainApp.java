package com.mistrapitos.app;

import com.mistrapitos.utils.DatabaseUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.input.KeyCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Clase principal de la aplicación JavaFX.
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    /**
     * Método principal que inicia la aplicación JavaFX.
     * @param args Argumentos de línea de comandos
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Método que se ejecuta al iniciar la aplicación JavaFX.
     * @param primaryStage Escenario principal de la aplicación
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Inicializar la conexión a la base de datos
            DatabaseUtil.initialize();

            // Cargar la vista de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            // Obtener dimensiones de la pantalla
            double screenWidth = Screen.getPrimary().getBounds().getWidth();
            double screenHeight = Screen.getPrimary().getBounds().getHeight();

            // Configurar la escena para que se adapte al tamaño de la pantalla
            Scene scene = new Scene(root, screenWidth, screenHeight);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            // Configurar el escenario
            primaryStage.setTitle("Mis Trapitos - Sistema de Gestión");
            primaryStage.setScene(scene);
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.jpg")));

            // Establecer límites mínimos como ya tenías
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(768);

            // --- CAMBIO PRINCIPAL: Pantalla completa real ---
            primaryStage.setFullScreen(true); // Pantalla completa real
            primaryStage.setFullScreenExitHint(""); // Quita el mensaje de "ESC para salir"
            primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // Opcional: desactiva ESC para salir

            // Centrar en pantalla para asegurar posicionamiento correcto
            primaryStage.centerOnScreen();

            primaryStage.show();

            logger.info("Aplicación iniciada a pantalla completa: {}x{}", screenWidth, screenHeight);
        } catch (IOException e) {
            logger.error("Error al iniciar la aplicación", e);
            System.exit(1);
        }
    }

    /**
     * Método que se ejecuta al cerrar la aplicación.
     */
    @Override
    public void stop() {
        // Cerrar la conexión a la base de datos
        DatabaseUtil.close();
        logger.info("Aplicación cerrada correctamente");
    }
}
