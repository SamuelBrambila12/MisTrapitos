package com.mistrapitos.controllers;

import com.mistrapitos.models.Usuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.io.InputStream;

public class MainController {

    @FXML private Label usuarioLabel;
    @FXML private Button logoutButton;
    @FXML private StackPane contentPane;

    private Usuario usuarioActual;

    // Método utilitario para obtener el Stage principal
    private Stage getMainStage() {
        return (Stage) logoutButton.getScene().getWindow();
    }

    @FXML
    public void initialize() {
        // Cargar la vista de productos por defecto al iniciar
        Platform.runLater(() -> cargarVista("/fxml/Producto.fxml"));
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        usuarioLabel.setText(usuario.getNombreUsuario() + " (" + usuario.getRol() + ")");
    }

    /**
     * Al cerrar sesión, se muestra la ventana de Login y se cierra la ventana actual.
     */
    @FXML
    private void onLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea cerrar sesión?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Cerrar sesión");
        // --- Asegura que el Alert esté sobre la pantalla completa ---
        confirm.initOwner(getMainStage());
        confirm.initModality(Modality.WINDOW_MODAL);
        confirm.setOnShown(evt -> {
            Stage stage = (Stage) confirm.getDialogPane().getScene().getWindow();
            try {
                InputStream imageStream = getClass().getResourceAsStream("/images/logo.jpg");
                if (imageStream != null) {
                    stage.getIcons().add(new Image(imageStream));
                } else {
                    System.err.println("No se pudo cargar la imagen del logo");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                // Cargar la ventana de Login
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
                    Parent loginRoot = loader.load();
                    Scene loginScene = new Scene(loginRoot);

                    // Agregar el CSS
                    loginScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

                    // Crear un nuevo Stage para el Login
                    Stage loginStage = new Stage();
                    loginStage.setTitle("Mis Trapitos - Login");
                    loginStage.setScene(loginScene);
                    // --- Pantalla completa real al regresar al login ---
                    loginStage.setFullScreen(true);
                    loginStage.setFullScreenExitHint("");
                    loginStage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
                    loginStage.show();
                } catch (Exception ex) {
                    mostrarError("Error al cargar la ventana de Login:\n" + ex.getMessage());
                }

                // Cerrar la ventana actual (Main)
                Stage currentStage = getMainStage();
                currentStage.close();
            }
        });
    }

    @FXML
    private void onProductos() { cargarVista("/fxml/Producto.fxml"); }

    @FXML
    private void onUsuarios() { cargarVista("/fxml/Usuario.fxml"); }

    @FXML
    private void onVentas() { cargarVista("/fxml/Venta.fxml"); }

    @FXML
    private void onClientes() { cargarVista("/fxml/Cliente.fxml"); }

    @FXML
    private void onProveedores() { cargarVista("/fxml/Proveedor.fxml"); }

    @FXML
    private void onPromociones() { cargarVista("/fxml/Promocion.fxml"); }

    @FXML
    private void onReportes() { cargarVista("/fxml/Reporte.fxml"); }

    /**
     * Carga una vista FXML en el área central, pasando el usuario actual si el controlador lo soporta.
     * @param fxmlPath Ruta del archivo FXML
     */
    private void cargarVista(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent vista = loader.load();

            // Si el controlador de la vista cargada tiene setUsuarioActual, pásale el usuario
            Object controller = loader.getController();
            if (controller != null && usuarioActual != null) {
                try {
                    controller.getClass().getMethod("setUsuarioActual", Usuario.class)
                            .invoke(controller, usuarioActual);
                } catch (NoSuchMethodException ignored) {
                    // El controlador no requiere usuario actual, continuar normalmente
                }
            }

            contentPane.getChildren().setAll(vista);
        } catch (Exception e) {
            // Imprime el stack trace en la consola para depuración
            e.printStackTrace();
            mostrarError("No se pudo cargar la vista: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alert.setHeaderText("Error");
        // --- Asegura que el Alert esté sobre la pantalla completa ---
        alert.setOnShown(evt -> {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });
        alert.initOwner(getMainStage());
        alert.initModality(Modality.WINDOW_MODAL);
        alert.showAndWait();
    }
}
