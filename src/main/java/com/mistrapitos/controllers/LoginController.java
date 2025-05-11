
package com.mistrapitos.controllers;

import com.mistrapitos.models.Usuario;
import com.mistrapitos.services.UsuarioService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.input.KeyCombination;

import java.io.IOException;
import java.util.Optional;

public class LoginController {

    @FXML private TextField usuarioField;
    @FXML private PasswordField contrasenaField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private final UsuarioService usuarioService = new UsuarioService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        usuarioField.setOnAction(e -> contrasenaField.requestFocus());
        contrasenaField.setOnAction(e -> onLogin(null));
    }

    @FXML
    private void onLogin(ActionEvent event) {
        String usuario = usuarioField.getText();
        String contrasena = contrasenaField.getText();

        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.isBlank()) {
            mostrarError("Ingrese usuario y contraseña.");
            return;
        }

        Optional<Usuario> userOpt = usuarioService.autenticar(usuario, contrasena);
        if (userOpt.isPresent()) {
            Usuario user = userOpt.get();
            Stage loginStage = (Stage) loginButton.getScene().getWindow();
            boolean ventanaAbierta = false;
            if ("ADMIN".equalsIgnoreCase(user.getRol())) {
                ventanaAbierta = abrirVentanaPrincipal(user);
            } else if ("VENDEDOR".equalsIgnoreCase(user.getRol())) {
                ventanaAbierta = abrirPanelVentas(user);
            } else {
                mostrarError("Rol no soportado.");
            }
            if (ventanaAbierta) {
                loginStage.close();
            }
        } else {
            mostrarError("Usuario o contraseña incorrectos.");
        }
    }

    private void mostrarError(String mensaje) {
        errorLabel.setText(mensaje);
        errorLabel.setVisible(true);
    }

    private boolean abrirVentanaPrincipal(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
            Parent root = loader.load();
            MainController mainController = loader.getController();
            mainController.setUsuarioActual(usuario);

            Stage stage = new Stage();
            stage.setTitle("Mis Trapitos - Sistema de Gestión");
            stage.setScene(new Scene(root));
            // --- Pantalla completa real ---
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.show();
            return true;
        } catch (IOException e) {
            e.printStackTrace(); // <-- Para ver el error real en consola
            mostrarError("Error al abrir la ventana principal.");
            return false;
        }
    }

    private boolean abrirPanelVentas(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VentaPanel.fxml"));
            Parent root = loader.load();
            VentaPanelController ventaController = loader.getController();
            ventaController.setUsuarioActual(usuario);

            Stage stage = new Stage();
            stage.setTitle("Mis Trapitos - Panel de Ventas");
            stage.setScene(new Scene(root));
            // --- Pantalla completa real ---
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.show();
            return true;
        } catch (IOException e) {
            e.printStackTrace(); // <-- Para ver el error real en consola
            mostrarError("Error al abrir el panel de ventas.");
            return false;
        }
    }

    // --- Método para cerrar el programa al pulsar el botón ---
    @FXML
    private void onCerrarPrograma() {
        System.exit(0);
    }
}
