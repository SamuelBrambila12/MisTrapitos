package com.mistrapitos.controllers;

import com.mistrapitos.models.Categoria;
import com.mistrapitos.models.Producto;
import com.mistrapitos.services.ProductoService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProductoController {
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, String> colTallas;
    @FXML private TableColumn<Producto, String> colColores;
    @FXML private TableColumn<Producto, Double> colDescuento;
    @FXML private TableColumn<Producto, Void> colAcciones;
    @FXML private TextField buscarField;

    private final ProductoService productoService = new ProductoService();
    private final Logger logger = LoggerFactory.getLogger(ProductoController.class);

    // Fuente de verdad de productos
    private final ObservableList<Producto> productosOriginales = FXCollections.observableArrayList();

    // Lista observable de categorías para todos los ComboBox
    private final ObservableList<Categoria> categoriasObservable = FXCollections.observableArrayList();

    // Método utilitario para obtener el Stage principal
    private Stage getMainStage() {
        return (Stage) tablaProductos.getScene().getWindow();
    }

    @FXML
    public void initialize() {
        // Inicializa la tabla con una lista vacía para mantener la referencia
        tablaProductos.setItems(FXCollections.observableArrayList());

        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(cell -> cell.getValue().categoriaNombreProperty());
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colTallas.setCellValueFactory(new PropertyValueFactory<>("sizes"));
        colColores.setCellValueFactory(new PropertyValueFactory<>("colors"));
        colDescuento.setCellValueFactory(new PropertyValueFactory<>("descuento"));
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

        cargarCategorias();
        cargarProductos();

        // Búsqueda dinámica/autocompletada
        buscarField.textProperty().addListener((obs, oldVal, newVal) -> filtrarProductos(newVal));
    }

    private void cargarProductos() {
        List<Producto> productos = productoService.obtenerTodos();
        productosOriginales.setAll(productos); // No cambies la referencia
        tablaProductos.getItems().setAll(productosOriginales);
    }

    private void cargarCategorias() {
        categoriasObservable.setAll(productoService.obtenerTodasCategorias());
    }

    private void filtrarProductos(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            tablaProductos.getItems().setAll(productosOriginales);
            return;
        }

        String filtroLower = filtro.toLowerCase();

        // Verificar si el filtro es un número (posible precio)
        if (filtroLower.matches("\\d+(\\.\\d+)?")) {
            try {
                double precioMinimo = Double.parseDouble(filtroLower);

                // Filtrar productos con precio igual o mayor al valor ingresado
                List<Producto> filtrados = productosOriginales.stream()
                        .filter(p -> p.getPrecio() >= precioMinimo)
                        .collect(Collectors.toList());

                tablaProductos.getItems().setAll(filtrados);
                return;
            } catch (NumberFormatException e) {
                logger.warn("Error al parsear precio: {}", e.getMessage());
            }
        }

        // Filtrado normal por texto si no es un número
        Predicate<Producto> pred = p -> (p.getNombre() != null && p.getNombre().toLowerCase().contains(filtroLower))
                || (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(filtroLower))
                || (p.getCategoriaNombre() != null && p.getCategoriaNombre().toLowerCase().contains(filtroLower));

        List<Producto> filtrados = productosOriginales.stream().filter(pred).collect(Collectors.toList());
        tablaProductos.getItems().setAll(filtrados);

        // Autocompletado: si hay solo una coincidencia y el nombre empieza igual, autocompleta
        if (!filtrados.isEmpty()) {
            for (Producto p : filtrados) {
                String nombre = p.getNombre();
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
    private void onNuevoProducto() {
        Optional<Producto> result = mostrarDialogoProducto(null);
        result.ifPresent(nuevo -> {
            Producto guardado = productoService.guardar(nuevo);
            boolean exito = guardado != null && guardado.getIdProducto() > 0;
            if (exito) {
                mostrarAlerta("Producto agregado correctamente.", Alert.AlertType.INFORMATION);
                cargarProductos();
            } else {
                mostrarAlerta("No se pudo agregar el producto.", Alert.AlertType.ERROR);
            }
        });
    }

    private void onEditar(Producto producto) {
        Optional<Producto> result = mostrarDialogoProducto(producto);
        result.ifPresent(editado -> {
            editado.setIdProducto(producto.getIdProducto());
            productoService.actualizar(editado);
            cargarProductos();
            mostrarAlerta("Producto actualizado correctamente.", Alert.AlertType.INFORMATION);
        });
    }

    private void onEliminar(Producto producto) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar el producto seleccionado?", ButtonType.YES, ButtonType.NO);
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
                productoService.eliminar(producto.getIdProducto());
                cargarProductos();
                mostrarAlerta("Producto eliminado.", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void onNuevaCategoria() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva Categoría");
        dialog.setHeaderText("Registrar nueva categoría");
        dialog.setContentText("Nombre de la categoría:");
        // --- Asegura que el Dialog esté sobre la pantalla completa ---
        dialog.initOwner(getMainStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnShown(evt -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombre -> {
            nombre = nombre.trim();
            if (nombre.isEmpty()) {
                mostrarAlerta("El nombre de la categoría es obligatorio.", Alert.AlertType.ERROR);
                return;
            }
            Categoria nueva = productoService.guardarCategoria(nombre);
            if (nueva != null && nueva.getIdCategoria() > 0) {
                cargarCategorias(); // Actualiza la lista observable
                mostrarAlerta("Categoría creada correctamente.", Alert.AlertType.INFORMATION);
            } else {
                mostrarAlerta("No se pudo crear la categoría (¿ya existe?).", Alert.AlertType.ERROR);
            }
        });
    }

    private Optional<Producto> mostrarDialogoProducto(Producto productoEditar) {
        Dialog<Producto> dialog = new Dialog<>();
        dialog.setTitle(productoEditar == null ? "Registrar nuevo producto" : "Editar producto");
        dialog.setHeaderText(productoEditar == null ? "Ingrese los datos del nuevo producto:" : "Edite los datos del producto:");

        Label lblNombre = new Label("Nombre:");
        TextField tfNombre = new TextField();
        tfNombre.setPromptText("Nombre del producto");

        Label lblDescripcion = new Label("Descripción:");
        TextField tfDescripcion = new TextField();
        tfDescripcion.setPromptText("Descripción");

        Label lblCategoria = new Label("Categoría:");
        ComboBox<Categoria> cbCategoria = new ComboBox<>();
        cbCategoria.setPromptText("Seleccione una categoría");
        cbCategoria.setItems(categoriasObservable); // Usa la lista observable

        Label lblPrecio = new Label("Precio:");
        TextField tfPrecio = new TextField();
        tfPrecio.setPromptText("Precio");

        Label lblStock = new Label("Stock:");
        TextField tfStock = new TextField();
        tfStock.setPromptText("Stock");

        Label lblTallas = new Label("Tallas:");
        TextField tfTallas = new TextField();
        tfTallas.setPromptText("Ej: S,M,L,XL");

        Label lblColores = new Label("Colores:");
        TextField tfColores = new TextField();
        tfColores.setPromptText("Ej: Rojo,Azul,Verde");

        Label lblDescuento = new Label("Descuento (%):");
        TextField tfDescuento = new TextField();
        tfDescuento.setPromptText("Descuento");

        Label lblBarcode = new Label("Código de barras:");
        TextField tfBarcode = new TextField();
        tfBarcode.setPromptText("Código de barras");

        if (productoEditar != null) {
            tfNombre.setText(productoEditar.getNombre());
            tfDescripcion.setText(productoEditar.getDescripcion());
            cbCategoria.getItems().stream()
                    .filter(cat -> cat.getNombre().equals(productoEditar.getCategoriaNombre()))
                    .findFirst()
                    .ifPresent(cbCategoria::setValue);
            tfPrecio.setText(String.valueOf(productoEditar.getPrecio()));
            tfStock.setText(String.valueOf(productoEditar.getStock()));
            tfTallas.setText(productoEditar.getSizes());
            tfColores.setText(productoEditar.getColors());
            tfDescuento.setText(String.valueOf(productoEditar.getDescuento()));
            tfBarcode.setText(productoEditar.getBarcode());
        }

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(lblNombre, tfNombre, lblDescripcion, tfDescripcion, lblCategoria, cbCategoria,
                lblPrecio, tfPrecio, lblStock, tfStock, lblTallas, tfTallas, lblColores, tfColores,
                lblDescuento, tfDescuento, lblBarcode, tfBarcode);
        dialog.getDialogPane().setContent(vbox);

        ButtonType guardarBtn = new ButtonType(productoEditar == null ? "Registrar" : "Actualizar", ButtonBar.ButtonData.OK_DONE);
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
                try {
                    String nombre = tfNombre.getText().trim();
                    String descripcion = tfDescripcion.getText().trim();
                    Categoria categoria = cbCategoria.getValue();
                    double precio = Double.parseDouble(tfPrecio.getText().trim());
                    int stock = Integer.parseInt(tfStock.getText().trim());
                    String tallas = tfTallas.getText().trim();
                    String colores = tfColores.getText().trim();
                    double descuento = Double.parseDouble(tfDescuento.getText().trim());
                    String barcode = tfBarcode.getText().trim();

                    if (nombre.isEmpty() || categoria == null) {
                        mostrarAlerta("El nombre y la categoría son obligatorios.", Alert.AlertType.ERROR);
                        return null;
                    }
                    Producto nuevo = productoEditar == null ? new Producto() : new Producto();
                    nuevo.setNombre(nombre);
                    nuevo.setDescripcion(descripcion);
                    nuevo.setIdCategoria(categoria.getIdCategoria());
                    nuevo.setCategoriaNombre(categoria.getNombre());
                    nuevo.setPrecio(precio);
                    nuevo.setStock(stock);
                    nuevo.setSizes(tallas);
                    nuevo.setColors(colores);
                    nuevo.setDescuento(descuento);
                    nuevo.setBarcode(barcode);
                    if (productoEditar != null) {
                        nuevo.setIdProducto(productoEditar.getIdProducto());
                    }
                    return nuevo;
                } catch (Exception e) {
                    mostrarAlerta("Datos inválidos. Verifique los campos.", Alert.AlertType.ERROR);
                    return null;
                }
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
