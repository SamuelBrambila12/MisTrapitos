package com.mistrapitos.controllers;

import com.mistrapitos.models.PromocionVista;
import com.mistrapitos.services.PromocionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class PromocionController {

    @FXML private TableView<PromocionVista> tablaPromociones;
    @FXML private TableColumn<PromocionVista, Integer> colIdProducto;
    @FXML private TableColumn<PromocionVista, String> colProducto;
    @FXML private TableColumn<PromocionVista, Double> colDescuentoDirecto;
    @FXML private TableColumn<PromocionVista, Double> colDescuentoPromo;
    @FXML private TableColumn<PromocionVista, LocalDate> colInicio;
    @FXML private TableColumn<PromocionVista, LocalDate> colFin;
    @FXML private TextField buscarField;

    private final PromocionService promocionService = new PromocionService();
    private final ObservableList<PromocionVista> promociones = FXCollections.observableArrayList();

    // Método utilitario para obtener el Stage principal
    private Stage getMainStage() {
        return (Stage) tablaPromociones.getScene().getWindow();
    }

    @FXML
    public void initialize() {
        // Configuración de celdas
        colIdProducto.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colDescuentoDirecto.setCellValueFactory(new PropertyValueFactory<>("descuentoDirecto"));
        colDescuentoPromo.setCellValueFactory(new PropertyValueFactory<>("porcentajePromocion"));
        colInicio.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("fechaFin"));

        // Formato de fechas dd/MM/yyyy
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colInicio.setCellFactory(col -> new TableCell<PromocionVista, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? "" : date.format(formatter));
            }
        });
        colFin.setCellFactory(col -> new TableCell<PromocionVista, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? "" : date.format(formatter));
            }
        });

        // Ordenar por descuento de promoción descendente
        colDescuentoPromo.setSortType(TableColumn.SortType.DESCENDING);

        tablaPromociones.setItems(promociones);
        cargarPromociones();

        // Aplicar orden automáticamente
        tablaPromociones.getSortOrder().setAll(colDescuentoPromo);
        tablaPromociones.sort();

        // Búsqueda dinámica y autocompletado
        buscarField.textProperty().addListener((obs, oldVal, newVal) -> filtrarPromociones(newVal));
    }

    private void cargarPromociones() {
        promociones.setAll(promocionService.obtenerPromocionesYDescuentos());
    }

    private void filtrarPromociones(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            promociones.setAll(promocionService.obtenerPromocionesYDescuentos());
            return;
        }
        List<PromocionVista> filtradas = promocionService.buscarPromocionesYDescuentosPorNombre(filtro);
        promociones.setAll(filtradas);
        // Autocompletado si solo una coincidencia que empiece igual
        if (!filtradas.isEmpty()) {
            String primer = filtradas.get(0).getNombreProducto();
            if (primer.toLowerCase().startsWith(filtro.toLowerCase()) && !primer.equalsIgnoreCase(filtro)) {
                buscarField.setText(primer);
                buscarField.positionCaret(primer.length());
                buscarField.selectRange(filtro.length(), primer.length());
            }
        }
    }

    @FXML
    private void onNuevaPromocion() {
        PromocionVista sel = tablaPromociones.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarError("Seleccione un producto para editar su promoción.");
            return;
        }
        Optional<PromocionVista> resp = mostrarDialogoPromocion(sel);
        resp.ifPresent(edit -> {
            if (promocionService.guardarPromocionYDescuento(edit)) {
                mostrarInfo("Promoción guardada correctamente.");
                cargarPromociones();
                tablaPromociones.getSortOrder().setAll(colDescuentoPromo);
                tablaPromociones.sort();
            } else mostrarError("No se pudo guardar la promoción.");
        });
    }

    private Optional<PromocionVista> mostrarDialogoPromocion(PromocionVista promocion) {
        Dialog<PromocionVista> dialog = new Dialog<>();
        dialog.setTitle("Editar Promoción");

        Label lbl = new Label("Producto: " + promocion.getNombreProducto());
        TextField tfDirecto = new TextField(promocion.getDescuentoDirecto() == null ? "0" : promocion.getDescuentoDirecto().toString());
        tfDirecto.setPromptText("Descuento directo (%)");
        TextField tfPromo = new TextField(promocion.getPorcentajePromocion() == null ? "0" : promocion.getPorcentajePromocion().toString());
        tfPromo.setPromptText("Descuento promoción (%)");
        DatePicker dpInicio = new DatePicker(promocion.getFechaInicio());
        DatePicker dpFin = new DatePicker(promocion.getFechaFin());

        VBox vb = new VBox(10, lbl, new Label("Descuento directo (%)"), tfDirecto,
                new Label("Descuento promoción (%)"), tfPromo,
                new Label("Fecha inicio"), dpInicio,
                new Label("Fecha fin"), dpFin);
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // --- Asegura que el Dialog esté sobre la pantalla completa ---
        dialog.initOwner(getMainStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnShown(evt -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    double dDirecto = Double.parseDouble(tfDirecto.getText());
                    double dPromo = Double.parseDouble(tfPromo.getText());
                    LocalDate ini = dpInicio.getValue();
                    LocalDate fin = dpFin.getValue();
                    if (dDirecto < 0 || dDirecto > 100) throw new IllegalArgumentException();
                    if (dPromo < 0 || dPromo > 100) throw new IllegalArgumentException();
                    if (dPromo > 0 && (ini == null || fin == null || fin.isBefore(ini))) throw new IllegalArgumentException();
                    return new PromocionVista(
                            promocion.getIdProducto(),
                            promocion.getNombreProducto(),
                            dDirecto,
                            promocion.getIdPromocion(),
                            dPromo > 0 ? dPromo : null,
                            dPromo > 0 ? ini : null,
                            dPromo > 0 ? fin : null
                    );
                } catch (Exception e) {
                    mostrarError("Datos inválidos, verifique los valores.");
                }
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private void mostrarError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        // --- Asegura que el Alert esté sobre la pantalla completa ---
        a.setOnShown(evt -> {
            Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });
        a.initOwner(getMainStage());
        a.initModality(Modality.WINDOW_MODAL);
        a.showAndWait();
    }

    private void mostrarInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText("Información");
        // --- Asegura que el Alert esté sobre la pantalla completa ---
        a.setOnShown(evt -> {
            Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });
        a.initOwner(getMainStage());
        a.initModality(Modality.WINDOW_MODAL);
        a.showAndWait();
    }
}
