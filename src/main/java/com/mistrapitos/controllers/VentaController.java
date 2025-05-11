package com.mistrapitos.controllers;

import com.mistrapitos.models.Venta;
import com.mistrapitos.models.DetalleVenta;
import com.mistrapitos.services.VentaService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VentaController {

    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, Integer> colId;
    @FXML private TableColumn<Venta, String> colCliente;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, String> colMetodoPago;
    @FXML private TableColumn<Venta, Double> colTotal;
    @FXML private TableColumn<Venta, Void> colAcciones;
    @FXML private TextField buscarField;

    private final VentaService ventaService = new VentaService();
    // Fuente de verdad de ventas (mantiene la referencia de la lista original)
    private final ObservableList<Venta> ventasOriginales = FXCollections.observableArrayList();

    // Definimos un formateador para la fecha en el formato "dd/MM/yyyy - hh:mm a"
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm a");

    // Método utilitario para obtener el Stage principal
    private Stage getMainStage() {
        return (Stage) tablaVentas.getScene().getWindow();
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cell -> cell.getValue().idVentaProperty().asObject());
        colCliente.setCellValueFactory(cell -> cell.getValue().clienteNombreProperty());

        // Utilizamos el formateador para la columna de la fecha
        colFecha.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getFecha().format(dateFormatter))
        );

        colMetodoPago.setCellValueFactory(cell -> cell.getValue().metodoPagoProperty());
        colTotal.setCellValueFactory(cell -> cell.getValue().totalProperty().asObject());

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button verBtn = new Button("Ver");
            private final Button eliminarBtn = new Button("Eliminar");
            {
                verBtn.getStyleClass().addAll("action-button", "ver-button");
                eliminarBtn.getStyleClass().addAll("action-button", "delete-button");
                verBtn.setOnAction(e -> onVer(getTableView().getItems().get(getIndex())));
                eliminarBtn.setOnAction(e -> onEliminar(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, verBtn, eliminarBtn));
                }
            }
        });

        cargarVentas();

        // Búsqueda dinámica/autocompletado
        buscarField.textProperty().addListener((obs, oldVal, newVal) -> filtrarVentas(newVal));
    }

    private void cargarVentas() {
        List<Venta> ventas = ventaService.obtenerTodas();
        // Usamos setAll para mantener la referencia
        ventasOriginales.setAll(ventas);
        tablaVentas.getItems().setAll(ventasOriginales);
    }

    /**
     * Filtra la tabla de ventas por cliente o método de pago, con autocompletado.
     * Se copia la lógica utilizada en ProductoController para que al borrar el campo de búsqueda
     * se muestren los productos completos (con sus botones de acción).
     */
    private void filtrarVentas(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            // Usamos getItems().setAll para volver a asignar la lista original
            tablaVentas.getItems().setAll(ventasOriginales);
            return;
        }
        String filtroLower = filtro.toLowerCase();
        Predicate<Venta> pred = v ->
                (v.getClienteNombre() != null && v.getClienteNombre().toLowerCase().contains(filtroLower))
                        || (v.getMetodoPago() != null && v.getMetodoPago().toLowerCase().contains(filtroLower));
        List<Venta> filtradas = ventasOriginales.stream().filter(pred).collect(Collectors.toList());
        // Se vuelve a setear la lista filtrada utilizando setAll para conservar la referencia y así que se muestren correctamente las celdas con botones
        tablaVentas.getItems().setAll(filtradas);

        // Autocompletado: si hay una coincidencia cuyo nombre de cliente empieza igual que lo ingresado, se autocompleta.
        if (!filtradas.isEmpty()) {
            for (Venta v : filtradas) {
                String nombre = v.getClienteNombre();
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
    private void onNuevaVenta() {
        mostrarAlerta("Funcionalidad de nueva venta no implementada en este ejemplo.", Alert.AlertType.INFORMATION);
    }

    /**
     * Muestra todos los detalles de la venta seleccionada en un diálogo.
     */
    private void onVer(Venta venta) {
        List<DetalleVenta> detalles = ventaService.obtenerDetallesVenta(venta.getIdVenta());
        StringBuilder sb = new StringBuilder();
        sb.append("ID Venta: ").append(venta.getIdVenta()).append("\n");
        sb.append("Cliente: ").append(venta.getClienteNombre()).append("\n");
        sb.append("Fecha: ").append(venta.getFecha().format(dateFormatter)).append("\n"); // Usamos nuestro formateador
        sb.append("Método de pago: ").append(venta.getMetodoPago()).append("\n");
        sb.append("Total: $").append(String.format("%.2f", venta.getTotal())).append("\n\n");
        sb.append("Productos:\n");
        sb.append(String.format("%-25s %-10s %-12s %-10s %-10s\n", "Producto", "Cantidad", "Precio", "Desc(%)", "Subtotal"));
        sb.append("--------------------------------------------------------------------------\n");
        for (DetalleVenta d : detalles) {
            sb.append(String.format("%-25s %-10d $%-11.2f %-10.2f $%-10.2f\n",
                    d.getProductoNombre(),
                    d.getCantidad(),
                    d.getPrecioUnitario(),
                    d.getDescuentoAplicado(),
                    d.getSubtotal()
            ));
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        alert.setHeaderText("Detalle de Venta #" + venta.getIdVenta());
        alert.getDialogPane().setMinWidth(600);
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

    /**
     * Elimina la venta seleccionada tras confirmación.
     */
    private void onEliminar(Venta venta) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar la venta seleccionada?", ButtonType.YES, ButtonType.NO);
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
                ventaService.eliminarVenta(venta.getIdVenta());
                cargarVentas();
                mostrarAlerta("Venta eliminada.", Alert.AlertType.INFORMATION);
            }
        });
    }

    /**
     * Muestra un mensaje de alerta.
     */
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
