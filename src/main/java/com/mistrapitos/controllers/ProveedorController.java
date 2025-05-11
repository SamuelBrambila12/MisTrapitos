package com.mistrapitos.controllers;

import com.mistrapitos.models.Producto;
import com.mistrapitos.models.Proveedor;
import com.mistrapitos.services.ProductoService;
import com.mistrapitos.services.ProveedorService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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

public class ProveedorController {

    @FXML private TableView<Proveedor> tablaProveedores;
    @FXML private TableColumn<Proveedor, Integer> colId;
    @FXML private TableColumn<Proveedor, String> colNombre;
    @FXML private TableColumn<Proveedor, String> colContacto;
    @FXML private TableColumn<Proveedor, String> colDireccion;
    @FXML private TableColumn<Proveedor, String> colTelefono;
    @FXML private TableColumn<Proveedor, String> colCorreo;
    @FXML private TableColumn<Proveedor, String> colProductosVendidos;
    @FXML private TableColumn<Proveedor, Void> colAcciones;
    @FXML private TextField buscarField;

    private final ProveedorService proveedorService = new ProveedorService();
    private ObservableList<Proveedor> proveedores = FXCollections.observableArrayList();

    // Método utilitario para obtener el Stage principal
    private Stage getMainStage() {
        return (Stage) tablaProveedores.getScene().getWindow();
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cell -> cell.getValue().idProveedorProperty().asObject());
        colNombre.setCellValueFactory(cell -> cell.getValue().nombreProperty());
        colContacto.setCellValueFactory(cell -> cell.getValue().contactoProperty());
        colDireccion.setCellValueFactory(cell -> cell.getValue().direccionProperty());
        colTelefono.setCellValueFactory(cell -> cell.getValue().telefonoProperty());
        colCorreo.setCellValueFactory(cell -> cell.getValue().correoProperty());
        colProductosVendidos.setCellValueFactory(cell -> cell.getValue().productosVendidosProperty());

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

        buscarField.setOnKeyReleased(this::onBuscarDinamico);

        cargarProveedores();
    }

    private void cargarProveedores() {
        proveedores = FXCollections.observableArrayList(proveedorService.obtenerTodos());
        tablaProveedores.setItems(proveedores);
    }

    @FXML
    private void onNuevoProveedor() {
        Optional<Proveedor> result = mostrarDialogoProveedor(null);
        result.ifPresent(nuevo -> {
            Proveedor guardado = proveedorService.guardar(nuevo);
            boolean exito = guardado != null && guardado.getIdProveedor() > 0;
            if (exito) {
                mostrarMensaje("Proveedor registrado correctamente.", true);
                cargarProveedores();
            } else {
                mostrarMensaje("No se pudo registrar el proveedor.", false);
            }
        });
    }

    private Optional<Proveedor> mostrarDialogoProveedor(Proveedor proveedorEditar) {
        Dialog<Proveedor> dialog = new Dialog<>();
        dialog.setTitle(proveedorEditar == null ? "Registrar nuevo proveedor" : "Editar proveedor");
        dialog.setHeaderText(proveedorEditar == null ? "Ingrese los datos del nuevo proveedor:" : "Edite los datos del proveedor:");

        // Campos de entrada
        Label lblNombre = new Label("Nombre:");
        TextField tfNombre = new TextField();
        tfNombre.setPromptText("Nombre del proveedor");

        Label lblContacto = new Label("Contacto:");
        TextField tfContacto = new TextField();
        tfContacto.setPromptText("Nombre del contacto");

        Label lblDireccion = new Label("Dirección:");
        TextField tfDireccion = new TextField();
        tfDireccion.setPromptText("Dirección completa");

        Label lblTelefono = new Label("Teléfono:");
        TextField tfTelefono = new TextField();
        tfTelefono.setPromptText("10 dígitos");

        Label lblCorreo = new Label("Correo:");
        TextField tfCorreo = new TextField();
        tfCorreo.setPromptText("ejemplo@correo.com");

        // Contenedor para checkboxes de productos
        Label lblProductos = new Label("Selecciona los productos que vende:");
        VBox productosCheckboxes = new VBox(5);
        ProductoService productoService = new ProductoService();
        List<Producto> productosDisponiblesList = productoService.obtenerTodos();
        for (Producto producto : productosDisponiblesList) {
            CheckBox cb = new CheckBox(producto.getNombre());
            productosCheckboxes.getChildren().add(cb);
        }

        Label lblProductosTxt = new Label("Productos seleccionados:");
        TextField tfProductos = new TextField();
        tfProductos.setEditable(false);

        Runnable actualizarSeleccion = () -> {
            List<String> seleccionados = productosCheckboxes.getChildren()
                    .stream()
                    .filter(node -> node instanceof CheckBox)
                    .map(node -> (CheckBox) node)
                    .filter(CheckBox::isSelected)
                    .map(CheckBox::getText)
                    .collect(Collectors.toList());
            String csv = String.join(", ", seleccionados);
            tfProductos.setText(csv);
        };

        productosCheckboxes.getChildren().forEach(node -> {
            if (node instanceof CheckBox) {
                ((CheckBox) node).selectedProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccion.run());
            }
        });

        if (proveedorEditar != null) {
            tfNombre.setText(proveedorEditar.getNombre());
            tfContacto.setText(proveedorEditar.getContacto());
            tfDireccion.setText(proveedorEditar.getDireccion());
            tfTelefono.setText(proveedorEditar.getTelefono());
            tfCorreo.setText(proveedorEditar.getCorreo());
            if (proveedorEditar.getProductosVendidos() != null && !proveedorEditar.getProductosVendidos().isEmpty()) {
                String csv = proveedorEditar.getProductosVendidos();
                tfProductos.setText(csv);
                List<String> nombresSeleccionados = List.of(csv.split(","))
                        .stream()
                        .map(String::trim)
                        .collect(Collectors.toList());
                productosCheckboxes.getChildren().forEach(node -> {
                    if (node instanceof CheckBox) {
                        CheckBox cb = (CheckBox) node;
                        if (nombresSeleccionados.contains(cb.getText())) {
                            cb.setSelected(true);
                        }
                    }
                });
            }
        }

        VBox vbox = new VBox(10,
                lblNombre, tfNombre,
                lblContacto, tfContacto,
                lblDireccion, tfDireccion,
                lblTelefono, tfTelefono,
                lblCorreo, tfCorreo,
                lblProductos, productosCheckboxes,
                lblProductosTxt, tfProductos);
        dialog.getDialogPane().setContent(vbox);

        ButtonType guardarBtn = new ButtonType(proveedorEditar == null ? "Registrar" : "Actualizar", ButtonBar.ButtonData.OK_DONE);
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
                String contacto = tfContacto.getText().trim();
                String direccion = tfDireccion.getText().trim();
                String telefono = tfTelefono.getText().trim();
                String correo = tfCorreo.getText().trim();
                if (nombre.isEmpty()) {
                    mostrarMensaje("El nombre es obligatorio.", false);
                    return null;
                }
                String csvProductos = tfProductos.getText().trim();
                Proveedor nuevo = proveedorEditar == null ?
                        new Proveedor() : new Proveedor(proveedorEditar.getIdProveedor(), nombre, contacto, direccion, telefono, correo, csvProductos);
                nuevo.setNombre(nombre);
                nuevo.setContacto(contacto);
                nuevo.setDireccion(direccion);
                nuevo.setTelefono(telefono);
                nuevo.setCorreo(correo);
                nuevo.setProductosVendidos(csvProductos);
                return nuevo;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void onBuscarDinamico(KeyEvent event) {
        String filtro = buscarField.getText().trim().toLowerCase();
        if (filtro.isEmpty()) {
            tablaProveedores.setItems(proveedores);
        } else {
            Predicate<Proveedor> pred = p -> {
                boolean coincideNombre = p.getNombre() != null && p.getNombre().toLowerCase().contains(filtro);
                boolean coincideProductos = p.getProductosVendidos() != null && p.getProductosVendidos().toLowerCase().contains(filtro);
                return coincideNombre || coincideProductos;
            };
            List<Proveedor> filtrados = proveedores.stream().filter(pred).collect(Collectors.toList());
            tablaProveedores.setItems(FXCollections.observableArrayList(filtrados));
        }
        tablaProveedores.refresh();
    }

    private void mostrarMensaje(String mensaje, boolean exito) {
        Alert alert = new Alert(exito ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alert.setHeaderText(null);
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

    private void onEditar(Proveedor proveedor) {
        Optional<Proveedor> result = mostrarDialogoProveedor(proveedor);
        result.ifPresent(editado -> {
            editado.setIdProveedor(proveedor.getIdProveedor());
            proveedorService.actualizar(editado);
            cargarProveedores();
            mostrarAlerta("Proveedor actualizado correctamente.", Alert.AlertType.INFORMATION);
        });
    }

    private void onEliminar(Proveedor proveedor) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar el proveedor seleccionado?", ButtonType.YES, ButtonType.NO);
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
                proveedorService.eliminar(proveedor.getIdProveedor());
                cargarProveedores();
                mostrarAlerta("Proveedor eliminado.", Alert.AlertType.INFORMATION);
            }
        });
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
