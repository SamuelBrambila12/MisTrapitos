
package com.mistrapitos.controllers;

import com.mistrapitos.models.Usuario;
import com.mistrapitos.services.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UsuarioController {

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, Integer> colId;
    @FXML private TableColumn<Usuario, String> colNombreUsuario;
    @FXML private TableColumn<Usuario, String> colContrasena;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, Void> colAcciones;
    @FXML private TextField buscarField;

    private final UsuarioService usuarioService = new UsuarioService();
    // Fuente de verdad de usuarios
    private final ObservableList<Usuario> usuariosOriginales = FXCollections.observableArrayList();

    // Método utilitario para obtener el Stage principal
    private Stage getMainStage() {
        return (Stage) tablaUsuarios.getScene().getWindow();
    }

    @FXML
    public void initialize() {
        // Inicializa la tabla con una lista vacía para mantener la referencia
        tablaUsuarios.setItems(FXCollections.observableArrayList());

        colId.setCellValueFactory(cell -> cell.getValue().idUsuarioProperty().asObject());
        colNombreUsuario.setCellValueFactory(cell -> cell.getValue().nombreUsuarioProperty());

        // Columna de contraseña con botón "ojo" para mostrar/ocultar
        colContrasena.setCellFactory(col -> new TableCell<Usuario, String>() {
            private final Button verBtn = new Button("👁");
            private boolean mostrando = false;

            {
                verBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                verBtn.setOnAction(e -> {
                    mostrando = !mostrando;
                    updateItem(getItem(), false);
                });
            }

            @Override
            protected void updateItem(String contrasena, boolean empty) {
                super.updateItem(contrasena, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Usuario usuario = (Usuario) getTableRow().getItem();
                    String realPass = usuario.getContrasena();
                    if (mostrando) {
                        setText(realPass);
                        verBtn.setText("🙈");
                    } else {
                        setText(realPass.replaceAll(".", "*"));
                        verBtn.setText("👁");
                    }
                    setGraphic(verBtn);
                }
            }
        });

        colRol.setCellValueFactory(cell -> cell.getValue().rolProperty());

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button editarBtn = new Button("Editar");
            private final Button eliminarBtn = new Button("Eliminar");
            {
                editarBtn.getStyleClass().addAll("action-button", "edit-button");
                eliminarBtn.getStyleClass().addAll("action-button", "delete-button");
                editarBtn.setOnAction(e -> onEditar(getTableView().getItems().get(getIndex())));
                eliminarBtn.setOnAction(e -> onEliminar(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5);
                    box.getChildren().addAll(editarBtn, eliminarBtn);
                    setGraphic(box);
                }
            }
        });


        cargarUsuarios();

        // Búsqueda dinámica/autocompletada
        buscarField.textProperty().addListener((obs, oldVal, newVal) -> filtrarUsuarios(newVal));
    }

    private void cargarUsuarios() {
        List<Usuario> usuarios = usuarioService.obtenerTodos();
        usuariosOriginales.setAll(usuarios); // No cambies la referencia
        tablaUsuarios.getItems().setAll(usuariosOriginales);
    }

    private void filtrarUsuarios(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            tablaUsuarios.getItems().setAll(usuariosOriginales);
            return;
        }
        String filtroLower = filtro.toLowerCase();
        Predicate<Usuario> pred = u ->
                (u.getNombreUsuario() != null && u.getNombreUsuario().toLowerCase().contains(filtroLower))
                        || (u.getRol() != null && u.getRol().toLowerCase().contains(filtroLower));
        List<Usuario> filtrados = usuariosOriginales.stream().filter(pred).collect(Collectors.toList());
        tablaUsuarios.getItems().setAll(filtrados);

        // Autocompletado: si hay solo una coincidencia y el nombre de usuario empieza igual, autocompleta
        if (!filtrados.isEmpty()) {
            for (Usuario u : filtrados) {
                String nombre = u.getNombreUsuario();
                if (nombre != null && nombre.toLowerCase().startsWith(filtroLower) && !nombre.equalsIgnoreCase(filtro)) {
                    buscarField.setText(nombre);
                    buscarField.positionCaret(nombre.length());
                    buscarField.selectRange(filtro.length(), nombre.length());
                    break;
                }
            }
        }
    }

    @FXML
    private void onNuevoUsuario() {
        Optional<Usuario> result = mostrarDialogoUsuario(null);
        result.ifPresent(nuevo -> {
            Usuario guardado = usuarioService.guardar(nuevo);
            boolean exito = guardado != null && guardado.getIdUsuario() > 0;
            if (exito) {
                mostrarAlerta("Usuario agregado correctamente.", Alert.AlertType.INFORMATION);
                cargarUsuarios();
            } else {
                mostrarAlerta("No se pudo agregar el usuario.", Alert.AlertType.ERROR);
            }
        });
    }

    private void onEditar(Usuario usuario) {
        Optional<Usuario> result = mostrarDialogoUsuario(usuario);
        result.ifPresent(editado -> {
            editado.setIdUsuario(usuario.getIdUsuario());
            usuarioService.actualizar(editado);
            cargarUsuarios();
            mostrarAlerta("Usuario actualizado correctamente.", Alert.AlertType.INFORMATION);
        });
    }

    private void onEliminar(Usuario usuario) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar el usuario seleccionado?", ButtonType.YES, ButtonType.NO);
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
                usuarioService.eliminar(usuario.getIdUsuario());
                cargarUsuarios();
                mostrarAlerta("Usuario eliminado.", Alert.AlertType.INFORMATION);
            }
        });
    }

    private Optional<Usuario> mostrarDialogoUsuario(Usuario usuarioEditar) {
        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle(usuarioEditar == null ? "Registrar nuevo usuario" : "Editar usuario");
        dialog.setHeaderText(usuarioEditar == null ? "Ingrese los datos del nuevo usuario:" : "Edite los datos del usuario:");

        Label lblNombre = new Label("Nombre de usuario:");
        TextField tfNombre = new TextField();
        tfNombre.setPromptText("Nombre de usuario");

        Label lblContrasena = new Label("Contraseña:");
        PasswordField pfContrasena = new PasswordField();
        pfContrasena.setPromptText("Contraseña");

        Label lblRol = new Label("Rol:");
        ComboBox<String> cbRol = new ComboBox<>(FXCollections.observableArrayList("ADMIN", "VENDEDOR"));
        cbRol.setPromptText("Rol");

        if (usuarioEditar != null) {
            tfNombre.setText(usuarioEditar.getNombreUsuario());
            pfContrasena.setText(usuarioEditar.getContrasena());
            cbRol.setValue(usuarioEditar.getRol());
        }

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(lblNombre, tfNombre, lblContrasena, pfContrasena, lblRol, cbRol);
        dialog.getDialogPane().setContent(vbox);

        ButtonType guardarBtn = new ButtonType(usuarioEditar == null ? "Registrar" : "Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        // --- Asegura que el Dialog esté sobre la pantalla completa ---
        dialog.initOwner(getMainStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnShown(evt -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarBtn) {
                String nombre = tfNombre.getText().trim();
                String contrasena = pfContrasena.getText().trim();
                String rol = cbRol.getValue();
                if (nombre.isEmpty() || contrasena.isEmpty() || rol == null || rol.isEmpty()) {
                    mostrarAlerta("Todos los campos son obligatorios.", Alert.AlertType.ERROR);
                    return null;
                }
                Usuario nuevo = usuarioEditar == null ? new Usuario() : new Usuario();
                nuevo.setNombreUsuario(nombre);
                nuevo.setContrasena(contrasena);
                nuevo.setRol(rol);
                if (usuarioEditar != null) {
                    nuevo.setIdUsuario(usuarioEditar.getIdUsuario());
                }
                return nuevo;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo, mensaje, ButtonType.OK);
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
