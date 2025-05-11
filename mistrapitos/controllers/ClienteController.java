
package com.mistrapitos.controllers;

import com.mistrapitos.models.Cliente;
import com.mistrapitos.services.ClienteService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.util.List;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClienteController {

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colDireccion;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colCiudad;
    @FXML private TableColumn<Cliente, Void> colAcciones;
    @FXML private TextField buscarField;

    private final ClienteService clienteService = new ClienteService();
    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();

    // Método utilitario para obtener el Stage principal
    private Stage getMainStage() {
        return (Stage) tablaClientes.getScene().getWindow();
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cell -> cell.getValue().idClienteProperty().asObject());
        colNombre.setCellValueFactory(cell -> cell.getValue().nombreProperty());
        colDireccion.setCellValueFactory(cell -> cell.getValue().direccionProperty());
        colCorreo.setCellValueFactory(cell -> cell.getValue().correoProperty());
        colTelefono.setCellValueFactory(cell -> cell.getValue().telefonoProperty());
        colCiudad.setCellValueFactory(cell -> cell.getValue().ciudadProperty());

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
                    setGraphic(new HBox(5, editarBtn, eliminarBtn));
                }
            }
        });

        buscarField.setOnKeyReleased(this::onBuscarAutocompletado);

        cargarClientes();
    }

    private void cargarClientes() {
        clientes.setAll(clienteService.obtenerTodos());
        tablaClientes.getItems().setAll(clientes);
    }

    private void onBuscarAutocompletado(KeyEvent event) {
        String filtro = buscarField.getText().trim().toLowerCase();
        if (filtro.isEmpty()) {
            tablaClientes.getItems().setAll(clientes);
        } else {
            Predicate<Cliente> pred = c -> c.getNombre() != null && c.getNombre().toLowerCase().contains(filtro);
            List<Cliente> filtrados = clientes.stream().filter(pred).collect(Collectors.toList());
            tablaClientes.getItems().setAll(FXCollections.observableArrayList(filtrados));
            if (filtrados.size() == 1) {
                String nombre = filtrados.get(0).getNombre();
                if (nombre != null && nombre.toLowerCase().startsWith(filtro) && !nombre.equalsIgnoreCase(filtro)) {
                    buscarField.setText(nombre);
                    buscarField.positionCaret(nombre.length());
                }
            }
        }
    }

    @FXML
    private void onNuevoCliente() {
        Optional<Cliente> result = mostrarDialogoCliente(null);
        result.ifPresent(nuevo -> {
            Cliente guardado = clienteService.guardar(nuevo);
            boolean exito = guardado != null && guardado.getIdCliente() > 0;
            if (exito) {
                mostrarAlerta("Cliente agregado correctamente.", Alert.AlertType.INFORMATION);
                cargarClientes();
            } else {
                mostrarAlerta("No se pudo agregar el cliente.", Alert.AlertType.ERROR);
            }
        });
    }

    private Optional<Cliente> mostrarDialogoCliente(Cliente clienteEditar) {
        Dialog<Cliente> dialog = new Dialog<>();
        dialog.setTitle(clienteEditar == null ? "Registrar nuevo cliente" : "Editar cliente");
        dialog.setHeaderText(clienteEditar == null ? "Ingrese los datos del nuevo cliente:" : "Edite los datos del cliente:");

        Label lblNombre = new Label("Nombre:");
        TextField tfNombre = new TextField();
        tfNombre.setPromptText("Nombre completo");

        Label lblDireccion = new Label("Dirección:");
        TextField tfDireccion = new TextField();
        tfDireccion.setPromptText("Dirección");

        Label lblCorreo = new Label("Correo:");
        TextField tfCorreo = new TextField();
        tfCorreo.setPromptText("ejemplo@correo.com");

        Label lblTelefono = new Label("Teléfono:");
        TextField tfTelefono = new TextField();
        tfTelefono.setPromptText("10 dígitos");

        Label lblCiudad = new Label("Ciudad:");
        TextField tfCiudad = new TextField();
        tfCiudad.setPromptText("Ciudad de residencia");

        if (clienteEditar != null) {
            tfNombre.setText(clienteEditar.getNombre());
            tfDireccion.setText(clienteEditar.getDireccion());
            tfCorreo.setText(clienteEditar.getCorreo());
            tfTelefono.setText(clienteEditar.getTelefono());
            tfCiudad.setText(clienteEditar.getCiudad());
        }

        VBox vbox = new VBox(10, lblNombre, tfNombre, lblDireccion, tfDireccion, lblCorreo, tfCorreo, lblTelefono, tfTelefono, lblCiudad, tfCiudad);
        dialog.getDialogPane().setContent(vbox);

        ButtonType guardarBtn = new ButtonType(clienteEditar == null ? "Registrar" : "Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        // --- MODIFICACIÓN: asegurar que el diálogo esté encima y modal ---
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
                String direccion = tfDireccion.getText().trim();
                String correo = tfCorreo.getText().trim();
                String telefono = tfTelefono.getText().trim();
                String ciudad = tfCiudad.getText().trim();
                if (nombre.isEmpty()) {
                    mostrarAlerta("El nombre es obligatorio.", Alert.AlertType.ERROR);
                    return null;
                }
                Cliente nuevo = clienteEditar == null
                        ? new Cliente()
                        : new Cliente(clienteEditar.getIdCliente(), nombre, direccion, correo, telefono, ciudad);
                if (clienteEditar == null) {
                    nuevo.setNombre(nombre);
                    nuevo.setDireccion(direccion);
                    nuevo.setCorreo(correo);
                    nuevo.setTelefono(telefono);
                    nuevo.setCiudad(ciudad);
                }
                return nuevo;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void onEditar(Cliente cliente) {
        Optional<Cliente> result = mostrarDialogoCliente(cliente);
        result.ifPresent(editado -> {
            editado.setIdCliente(cliente.getIdCliente());
            clienteService.actualizar(editado);
            cargarClientes();
            mostrarAlerta("Cliente actualizado correctamente.", Alert.AlertType.INFORMATION);
        });
    }

    private void onEliminar(Cliente cliente) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar el cliente seleccionado?", ButtonType.YES, ButtonType.NO);
        // --- MODIFICACIÓN: asegurar que el alert esté encima y modal ---
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
                clienteService.eliminar(cliente.getIdCliente());
                cargarClientes();
                mostrarAlerta("Cliente eliminado.", Alert.AlertType.INFORMATION);
            }
        });
    }

    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo, mensaje, ButtonType.OK);
        // --- MODIFICACIÓN: asegurar que el alert esté encima y modal ---
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
