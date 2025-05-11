package com.mistrapitos.controllers;

import com.mistrapitos.models.Cliente;
import com.mistrapitos.models.Producto;
import com.mistrapitos.models.ProductoEnCarrito;
import com.mistrapitos.models.Usuario;
import com.mistrapitos.models.Categoria;
import com.mistrapitos.services.ClienteService;
import com.mistrapitos.services.ProductoService;
import com.mistrapitos.services.VentaService;
import com.mistrapitos.utils.ReporteUtil;
import com.mistrapitos.utils.DatabaseUtil;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.collections.transformation.FilteredList;
import javafx.util.StringConverter;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import java.sql.SQLException;

import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.Random;

public class VentaPanelController {

    @FXML private Label usuarioLabel;
    @FXML private Button logoutButton;
    @FXML private TextField barcodeField;
    @FXML private Label productoInfoLabel;
    @FXML private TableView<ProductoEnCarrito> carritoTable;
    @FXML private TableColumn<ProductoEnCarrito, String> colProducto;
    @FXML private TableColumn<ProductoEnCarrito, Integer> colCantidad;
    @FXML private TableColumn<ProductoEnCarrito, Double> colPrecio;
    @FXML private TableColumn<ProductoEnCarrito, Double> colDescuento;
    @FXML private TableColumn<ProductoEnCarrito, Double> colSubtotal;
    @FXML private TableColumn<ProductoEnCarrito, Void> colAcciones;
    @FXML private TextField clienteField;
    @FXML private Label clienteInfoLabel;
    @FXML private ComboBox<String> metodoPagoCombo;
    @FXML private Label totalLabel;
    @FXML private Label mensajeLabel;
    @FXML private Button completarVentaBtn;

    // Nuevos botones (asegúrate de agregarlos en el FXML y asignar onAction)
    @FXML private Button agregarProductoBtn;
    @FXML private Button editarProductoBtn;
    @FXML private Button historialClienteBtn;

    // Card Payment Animation components
    @FXML private StackPane pagoTarjetaOverlay;
    @FXML private Label estadoTerminalLabel;
    @FXML private ProgressIndicator terminalProgress;
    @FXML private Label mensajeTerminalLabel;
    @FXML private Button cerrarTerminalBtn;

    private Usuario usuarioActual;
    private final ProductoService productoService = new ProductoService();
    private final VentaService ventaService = new VentaService();
    private final ClienteService clienteService = new ClienteService();
    private final ObservableList<ProductoEnCarrito> carrito = FXCollections.observableArrayList();
    private Cliente clienteSeleccionado = null;
    private Random random = new Random();

    private Stage getMainStage() {
        // Usamos cualquier nodo de la escena principal, aquí barcodeField
        return (Stage) barcodeField.getScene().getWindow();
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        usuarioLabel.setText(usuario.getNombreUsuario() + " (" + usuario.getRol() + ")");
    }

    @FXML
    public void initialize() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colDescuento.setCellValueFactory(new PropertyValueFactory<>("descuento"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        // Aplicar estilos especiales a columnas de precio
        colPrecio.getStyleClass().add("precio");
        colSubtotal.getStyleClass().add("subtotal");

        // Configurar anchos de columnas
        colProducto.prefWidthProperty().bind(carritoTable.widthProperty().multiply(0.35));
        colCantidad.prefWidthProperty().bind(carritoTable.widthProperty().multiply(0.10));
        colPrecio.prefWidthProperty().bind(carritoTable.widthProperty().multiply(0.15));
        colDescuento.prefWidthProperty().bind(carritoTable.widthProperty().multiply(0.15));
        colSubtotal.prefWidthProperty().bind(carritoTable.widthProperty().multiply(0.15));
        colAcciones.prefWidthProperty().bind(carritoTable.widthProperty().multiply(0.10));

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button eliminarBtn = new Button("Eliminar");
            {
                eliminarBtn.getStyleClass().add("button");
                eliminarBtn.setOnAction(e -> {
                    ProductoEnCarrito item = getTableView().getItems().get(getIndex());
                    carrito.remove(item);
                    actualizarTotal();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : eliminarBtn);
            }
        });

        carritoTable.setItems(carrito);
        actualizarTotal();

        // Inicializa el ComboBox de método de pago
        metodoPagoCombo.setItems(FXCollections.observableArrayList("Efectivo", "Tarjeta", "Transferencia"));
        metodoPagoCombo.setValue("Efectivo");

        // Configurar estilos iniciales
        mensajeLabel.setVisible(false);
        mensajeLabel.setManaged(false);

        Platform.runLater(() -> barcodeField.requestFocus());
    }


    @FXML
    private void onAgregarProducto() {
        String barcode = barcodeField.getText();
        if (barcode == null || barcode.isBlank()) {
            mostrarMensaje("Ingrese o escanee un código de barras.", false);
            return;
        }
        Optional<Producto> prodOpt = productoService.buscarPorBarcode(barcode);
        if (prodOpt.isEmpty()) {
            mostrarMensaje("Producto no encontrado.", false);
            productoInfoLabel.setText("");
            return;
        }
        Producto prod = prodOpt.get();
        productoInfoLabel.setText(prod.getNombre() + " | $" + prod.getPrecio() + " | Stock: " + prod.getStock());
        for (ProductoEnCarrito p : carrito) {
            if (p.getIdProducto() == prod.getIdProducto()) {
                if (p.getCantidad() < prod.getStock()) {
                    p.setCantidad(p.getCantidad() + 1);
                    carritoTable.refresh();
                    actualizarTotal();
                    mostrarMensaje("Cantidad aumentada.", true);
                } else {
                    mostrarMensaje("No hay suficiente stock.", false);
                }
                barcodeField.clear();
                barcodeField.requestFocus();
                return;
            }
        }
        if (prod.getStock() > 0) {
            carrito.add(new ProductoEnCarrito(prod, 1));
            actualizarTotal();
            mostrarMensaje("Producto agregado.", true);
        } else {
            mostrarMensaje("No hay stock disponible.", false);
        }
        barcodeField.clear();
        barcodeField.requestFocus();
    }

    @FXML
    private void onBuscarCliente() {
        String texto = clienteField.getText();
        if (texto == null || texto.isBlank()) {
            clienteInfoLabel.setText("");
            clienteSeleccionado = null;
            return;
        }
        Optional<Cliente> clienteOpt = clienteService.buscarPorNombreOId(texto);
        if (clienteOpt.isPresent()) {
            Cliente c = clienteOpt.get();
            clienteInfoLabel.setText("Cliente: " + c.getNombre() +
                    (c.getCorreo() != null ? " | " + c.getCorreo() : "") +
                    (c.getTelefono() != null ? " | " + c.getTelefono() : ""));
            clienteSeleccionado = c;
        } else {
            clienteInfoLabel.setText("No registrado");
            clienteSeleccionado = null;
        }
    }

    @FXML
    private void onRegistrarCliente() {
        Dialog<Cliente> dialog = new Dialog<>();
        dialog.setTitle("Registrar nuevo cliente");
        dialog.setHeaderText("Ingrese los datos del nuevo cliente:");

        DialogPane dialogPane = dialog.getDialogPane();
        // RUTA ABSOLUTA Y VERIFICACIÓN DE CSS
        try {
            java.net.URL cssUrl = getClass().getResource("/css/ventaPanel.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar el CSS para el diálogo de cliente: " + e.getMessage());
        }
        dialogPane.getStyleClass().add("dialog-pane");

        Label lblNombre = new Label("Nombre:");
        TextField tfNombre = new TextField();
        tfNombre.setPromptText("Nombre completo");

        Label lblCorreo = new Label("Correo electrónico:");
        TextField tfCorreo = new TextField();
        tfCorreo.setPromptText("ejemplo@correo.com");

        Label lblTelefono = new Label("Teléfono:");
        TextField tfTelefono = new TextField();
        tfTelefono.setPromptText("10 dígitos");

        Label lblDireccion = new Label("Dirección:");
        TextField tfDireccion = new TextField();
        tfDireccion.setPromptText("Calle, número, colonia...");

        Label lblCiudad = new Label("Ciudad:");
        TextField tfCiudad = new TextField();
        tfCiudad.setPromptText("Ciudad de residencia");

        VBox vbox = new VBox(10, lblNombre, tfNombre, lblCorreo, tfCorreo, lblTelefono, tfTelefono, lblDireccion, tfDireccion, lblCiudad, tfCiudad);
        vbox.setStyle("-fx-padding: 20;");
        dialog.getDialogPane().setContent(vbox);

        ButtonType guardarBtn = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        Platform.runLater(() -> {
            Button okButton = (Button) dialog.getDialogPane().lookupButton(guardarBtn);
            okButton.getStyleClass().add("button");
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarBtn) {
                String nombre = tfNombre.getText().trim();
                String correo = tfCorreo.getText().trim();
                String telefono = tfTelefono.getText().trim();
                String direccion = tfDireccion.getText().trim();
                String ciudad = tfCiudad.getText().trim();
                if (nombre.isEmpty()) {
                    mostrarMensaje("El nombre es obligatorio.", false);
                    return null;
                }
                Cliente nuevo = new Cliente();
                nuevo.setNombre(nombre);
                nuevo.setCorreo(correo);
                nuevo.setTelefono(telefono);
                nuevo.setDireccion(direccion);
                nuevo.setCiudad(ciudad);
                return nuevo;
            }
            return null;
        });

        dialog.initOwner(getMainStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnShown(evt -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });

        Optional<Cliente> result = dialog.showAndWait();
        result.ifPresent(nuevo -> {
            Cliente guardado = clienteService.registrarCliente(nuevo);
            if (guardado != null && guardado.getIdCliente() > 0) {
                clienteField.setText(String.valueOf(guardado.getIdCliente()));
                clienteInfoLabel.setText("Cliente: " + guardado.getNombre() +
                        (guardado.getCorreo() != null && !guardado.getCorreo().isBlank() ? " | " + guardado.getCorreo() : "") +
                        (guardado.getTelefono() != null && !guardado.getTelefono().isBlank() ? " | " + guardado.getTelefono() : "") +
                        (guardado.getDireccion() != null && !guardado.getDireccion().isBlank() ? " | " + guardado.getDireccion() : "") +
                        (guardado.getCiudad() != null && !guardado.getCiudad().isBlank() ? " | " + guardado.getCiudad() : ""));
                clienteSeleccionado = guardado;
                mostrarMensaje("Cliente registrado correctamente.", true);
            } else {
                mostrarMensaje("No se pudo registrar el cliente.", false);
            }
        });
    }

    @FXML
    private void onCompletarVenta() {
        if (carrito.isEmpty()) {
            mostrarMensaje("El carrito está vacío.", false);
            return;
        }
        if (clienteSeleccionado == null) {
            mostrarMensaje("Seleccione o registre un cliente.", false);
            return;
        }
        String metodoPago = metodoPagoCombo.getValue();
        if (metodoPago == null || metodoPago.isBlank()) {
            mostrarMensaje("Seleccione el método de pago.", false);
            return;
        }
        if ("Tarjeta".equals(metodoPago)) {
            mostrarAnimacionPagoTarjeta();
            return;
        }
        procesarVenta();
    }

    private void mostrarAnimacionPagoTarjeta() {
        pagoTarjetaOverlay.setVisible(true);
        pagoTarjetaOverlay.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), pagoTarjetaOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        estadoTerminalLabel.setText("Esperando respuesta de terminal...");
        mensajeTerminalLabel.setText("Por favor espere mientras se procesa el pago");
        cerrarTerminalBtn.setVisible(false);
        terminalProgress.setProgress(-1.0);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> {
                    boolean exito = random.nextInt(100) < 90;
                    if (exito) {
                        estadoTerminalLabel.setText("Pago Aprobado");
                        estadoTerminalLabel.setStyle("-fx-text-fill: #00b894; -fx-font-size: 18px; -fx-font-weight: bold;");
                        mensajeTerminalLabel.setText("La transacción ha sido aprobada por el banco");
                        terminalProgress.setProgress(1.0);
                        Timeline completarTimeline = new Timeline(
                                new KeyFrame(Duration.seconds(1.5), evt -> {
                                    FadeTransition fadeOut = new FadeTransition(Duration.millis(300), pagoTarjetaOverlay);
                                    fadeOut.setFromValue(1);
                                    fadeOut.setToValue(0);
                                    fadeOut.setOnFinished(event -> {
                                        procesarVenta();
                                        pagoTarjetaOverlay.setVisible(false);
                                        estadoTerminalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                                    });
                                    fadeOut.play();
                                })
                        );
                        completarTimeline.play();
                    } else {
                        estadoTerminalLabel.setText("Pago Rechazado");
                        estadoTerminalLabel.setStyle("-fx-text-fill: #d63031; -fx-font-size: 18px; -fx-font-weight: bold;");
                        mensajeTerminalLabel.setText("La transacción ha sido rechazada. Por favor intente con otro método de pago.");
                        terminalProgress.setProgress(0);
                        cerrarTerminalBtn.setVisible(true);
                    }
                })
        );
        timeline.play();
    }

    @FXML
    private void onCerrarTerminal() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), pagoTarjetaOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            pagoTarjetaOverlay.setVisible(false);
            estadoTerminalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        });
        fadeOut.play();
    }

    private void procesarVenta() {
        String metodoPago = metodoPagoCombo.getValue();
        boolean exito = ventaService.registrarVenta(carrito, clienteSeleccionado, usuarioActual, metodoPago);
        if (exito) {
            mostrarMensaje("Venta registrada correctamente. Imprima el ticket para entregarlo al cliente", true);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(0.7), evt -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Venta Exitosa");
                        alert.setHeaderText("Venta completada con éxito");
                        alert.setContentText("La venta ha sido registrada en el sistema.");
                        DialogPane dialogPane = alert.getDialogPane();
                        dialogPane.getStylesheets().add(getClass().getResource("/css/ventaPanel.css").toExternalForm());
                        dialogPane.getStyleClass().add("dialog-pane");
                        // MODIFICACIÓN: asegurar que el alert esté encima y modal
                        alert.setOnShown(windowEvent -> {
                            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                            stage.getIcons().add(
                                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
                            );
                        });
                        alert.initOwner(getMainStage());
                        alert.initModality(Modality.WINDOW_MODAL);
                        alert.showAndWait();
                        limpiarFormulario();
                    })
            );
            timeline.play();
        } else {
            mostrarMensaje("Error al registrar la venta.", false);
        }
    }


    private void limpiarFormulario() {
        // Verificación principal del hilo de JavaFX
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::limpiarFormulario);
            return;
        }

        try {
            // Limpiar carrito con verificación de nulidad
            if (carrito != null) {
                carrito.clear();
                // Actualizar la tabla para reflejar el carrito vacío
                carritoTable.refresh();
            }

            // Actualizar el total
            actualizarTotal();

            // Limpiar campos de cliente con verificación de nulidad
            if (clienteField != null) clienteField.clear();
            if (clienteInfoLabel != null) clienteInfoLabel.setText("");
            clienteSeleccionado = null;

            // Limpiar información de producto
            if (productoInfoLabel != null) productoInfoLabel.setText("");

            // Limpiar y enfocar el campo de código de barras
            if (barcodeField != null) {
                barcodeField.clear();
                // Aplazamos la solicitud de enfoque para asegurar que la UI esté actualizada
                Platform.runLater(() -> barcodeField.requestFocus());
            }

            // Asegurar que el método de pago predeterminado esté seleccionado
            if (metodoPagoCombo != null && metodoPagoCombo.getItems() != null && !metodoPagoCombo.getItems().isEmpty()) {
                metodoPagoCombo.setValue("Efectivo");
            }
        } catch (Exception e) {
            System.err.println("Error al limpiar el formulario: " + e.getMessage());
            e.printStackTrace();

            // Mostrar mensaje de error en la interfaz
            mostrarMensaje("Error al limpiar el formulario: " + e.getMessage(), false);
        }
    }

    @FXML
    private void onImprimirTicket() {
        if (carrito.isEmpty()) {
            mostrarMensaje("El carrito está vacío.", false);
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Ticket PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        // MODIFICACIÓN: usar getMainStage() como owner
        File file = fileChooser.showSaveDialog(getMainStage());
        if (file == null) return;
        new Thread(() -> {
            boolean exito = ReporteUtil.generarTicketVentaPDF(
                    carrito,
                    clienteSeleccionado != null ? clienteSeleccionado.getNombre() : "",
                    usuarioActual,
                    file.getAbsolutePath()
            );
            Platform.runLater(() -> {
                if (exito) {
                    mostrarMensaje("Ticket generado correctamente.", true);
                    limpiarFormulario();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Ticket Generado");
                    alert.setHeaderText("El ticket ha sido generado correctamente");
                    alert.setContentText("El archivo se ha guardado en la ubicación seleccionada.");
                    DialogPane dialogPane = alert.getDialogPane();
                    dialogPane.getStylesheets().add(getClass().getResource("/css/ventaPanel.css").toExternalForm());
                    dialogPane.getStyleClass().add("dialog-pane");
                    // MODIFICACIÓN: asegurar que el alert esté encima y modal
                    alert.setOnShown(evt -> {
                        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                        stage.getIcons().add(
                                new Image(getClass().getResourceAsStream("/images/logo.jpg"))
                        );
                    });
                    alert.initOwner(getMainStage());
                    alert.initModality(Modality.WINDOW_MODAL);
                    alert.show();
                } else {
                    mostrarMensaje("Error al generar el ticket.", false);
                }
            });
        }).start();
    }

    @FXML
    private void onConsultarInventario() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventario_consulta.fxml"));
            Parent root = loader.load();
            Stage inventarioStage = new Stage();
            inventarioStage.setTitle("Consulta de Inventario");
            // MODIFICACIÓN: asegurar que el stage esté encima y modal
            inventarioStage.initOwner(getMainStage());
            inventarioStage.initModality(Modality.WINDOW_MODAL);
            try {
                InputStream imageStream = getClass().getResourceAsStream("/images/logo.jpg");
                if (imageStream != null) {
                    inventarioStage.getIcons().add(new Image(imageStream));
                } else {
                    System.err.println("No se pudo cargar la imagen del logo");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            inventarioStage.show();
            Scene scene = new Scene(root);
            inventarioStage.setScene(scene);
            inventarioStage.show();
        } catch (IOException e) {
            System.err.println("Error al cargar la ventana de consulta de inventario: " + e.getMessage());
            mensajeLabel.setText("Error al abrir la consulta de inventario: " + e.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea cerrar sesión?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Cerrar sesión");

        try {
            DialogPane dialogPane = confirm.getDialogPane();
            String cssPath = getClass().getResource("/css/ventaPanel.css").toExternalForm();
            dialogPane.getStylesheets().add(cssPath);
            dialogPane.getStyleClass().add("dialog-pane");
        } catch (Exception e) {
            System.err.println("No se pudieron aplicar estilos al diálogo: " + e.getMessage());
        }

        // MODIFICACIÓN: asegurar que el alert esté encima y modal
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
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
                    Parent loginRoot = loader.load();
                    Scene loginScene = new Scene(loginRoot);

                    try {
                        loginScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                    } catch (Exception e) {
                        System.err.println("No se pudieron aplicar estilos a la escena: " + e.getMessage());
                    }

                    Stage loginStage = new Stage();
                    loginStage.setTitle("Mis Trapitos - Login");
                    loginStage.setScene(loginScene);
                    // --- Pantalla completa real al regresar al login ---
                    loginStage.setFullScreen(true);
                    loginStage.setFullScreenExitHint("");
                    loginStage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
                    loginStage.show();

                    Stage currentStage = getMainStage();
                    currentStage.close();
                } catch (Exception ex) {
                    System.err.println("Error al cargar la ventana de Login: " + ex.getMessage());
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "Error al cargar la ventana de Login:\n" + ex.getMessage(), ButtonType.OK);
                    // MODIFICACIÓN: asegurar que el alert esté encima y modal
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
        });
    }

    private void actualizarTotal() {
        double total = carrito.stream().mapToDouble(ProductoEnCarrito::getSubtotal).sum();
        totalLabel.setText(String.format("$ %.2f", total));
        FadeTransition ft = new FadeTransition(Duration.millis(200), totalLabel);
        ft.setFromValue(0.7);
        ft.setToValue(1.0);
        ft.play();
    }

    private void mostrarMensaje(String mensaje, boolean exito) {
        mensajeLabel.setText(mensaje);
        mensajeLabel.setStyle("-fx-text-fill: " + (exito ? "#00b894" : "#d63031"));
        mensajeLabel.setVisible(true);
        mensajeLabel.setManaged(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), mensajeLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(3.8), event -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(200), mensajeLabel);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> {
                        mensajeLabel.setVisible(false);
                        mensajeLabel.setManaged(false);
                    });
                    fadeOut.play();
                })
        );
        timeline.play();
    }

    // NUEVAS FUNCIONALIDADES

    // 1. Agregar Producto (Manual) con ComboBox de Categorías
    @FXML
    private void onAgregarProductoManual() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Agregar Producto");
        dialog.setHeaderText("Ingrese los datos del nuevo producto:");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField tfNombre = new TextField();
        tfNombre.setPromptText("Nombre");

        TextField tfDescripcion = new TextField();
        tfDescripcion.setPromptText("Descripción");

        // ComboBox con categorías
        ComboBox<Categoria> cbCategoria = new ComboBox<>();
        ObservableList<Categoria> listaCategorias = FXCollections.observableArrayList();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id_categoria, nombre FROM categorias ORDER BY nombre")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                listaCategorias.add(new Categoria(rs.getInt("id_categoria"), rs.getString("nombre")));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        cbCategoria.setItems(listaCategorias);
        cbCategoria.setPromptText("Seleccione Categoría");
        cbCategoria.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria object) {
                return object == null ? "" : object.getNombre();
            }
            @Override
            public Categoria fromString(String string) {
                return null;
            }
        });

        TextField tfPrecio = new TextField();
        tfPrecio.setPromptText("Precio");

        TextField tfStock = new TextField();
        tfStock.setPromptText("Stock");

        TextField tfSizes = new TextField();
        tfSizes.setPromptText("Sizes");

        TextField tfColors = new TextField();
        tfColors.setPromptText("Colors");

        TextField tfDescuento = new TextField();
        tfDescuento.setPromptText("Descuento");

        TextField tfBarcode = new TextField();
        tfBarcode.setPromptText("Código de Barras");

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(tfNombre, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(tfDescripcion, 1, 1);
        grid.add(new Label("Categoría:"), 0, 2);
        grid.add(cbCategoria, 1, 2);
        grid.add(new Label("Precio:"), 0, 3);
        grid.add(tfPrecio, 1, 3);
        grid.add(new Label("Stock:"), 0, 4);
        grid.add(tfStock, 1, 4);
        grid.add(new Label("Sizes:"), 0, 5);
        grid.add(tfSizes, 1, 5);
        grid.add(new Label("Colors:"), 0, 6);
        grid.add(tfColors, 1, 6);
        grid.add(new Label("Descuento:"), 0, 7);
        grid.add(tfDescuento, 1, 7);
        grid.add(new Label("Código de Barras:"), 0, 8);
        grid.add(tfBarcode, 1, 8);

        dialog.getDialogPane().setContent(grid);
        ButtonType btnAceptar = new ButtonType("Agregar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if(dialogButton == btnAceptar) {
                try {
                    String nombre = tfNombre.getText();
                    String descripcion = tfDescripcion.getText();
                    Categoria categoria = cbCategoria.getValue();
                    if(categoria == null) {
                        mostrarMensaje("Debe seleccionar una categoría.", false);
                        return null;
                    }
                    int idCategoria = categoria.getIdCategoria();
                    double precio = Double.parseDouble(tfPrecio.getText());
                    int stock = Integer.parseInt(tfStock.getText());
                    String sizes = tfSizes.getText();
                    String colors = tfColors.getText();
                    double descuento = Double.parseDouble(tfDescuento.getText());
                    String barcode = tfBarcode.getText();
                    boolean result = insertarProducto(nombre, descripcion, idCategoria, precio, stock, sizes, colors, descuento, barcode);
                    if (result) {
                        mostrarMensaje("Producto agregado correctamente.", true);
                    } else {
                        mostrarMensaje("Error al agregar el producto.", false);
                    }
                } catch (NumberFormatException ex) {
                    mostrarMensaje("Error en el formato de los campos numéricos.", false);
                }
            }
            return null;
        });
        dialog.initOwner(getMainStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnShown(evt -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });
        dialog.showAndWait();
    }

    private boolean insertarProducto(String nombre, String descripcion, int idCategoria, double precio, int stock, String sizes, String colors, double descuento, String barcode) {
        boolean exito = false;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO productos (nombre, descripcion, id_categoria, precio, stock, sizes, colors, descuento, barcode) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
             )) {
            stmt.setString(1, nombre);
            stmt.setString(2, descripcion);
            stmt.setInt(3, idCategoria);
            stmt.setDouble(4, precio);
            stmt.setInt(5, stock);
            stmt.setString(6, sizes);
            stmt.setString(7, colors);
            stmt.setDouble(8, descuento);
            stmt.setString(9, barcode);
            int rows = stmt.executeUpdate();
            exito = rows > 0;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return exito;
    }

    // 2. Editar Producto con autocompletar y formulario de actualización
    @FXML
    private void onEditarProducto() {
        // Crear el diálogo único que contendrá la búsqueda y el formulario de edición
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Editar Producto");
        dialog.setHeaderText("Busque y edite un producto");

        // Contenedor principal (VBox) para el diálogo
        VBox vboxCont = new VBox(15);
        vboxCont.setPadding(new Insets(20));

        // --- Parte de búsqueda (ComboBox editable con autocompletar) ---
        GridPane gridBusqueda = new GridPane();
        gridBusqueda.setHgap(10);
        gridBusqueda.setVgap(10);
        gridBusqueda.setPadding(new Insets(10));

        Label lblBuscar = new Label("Producto:");
        ComboBox<Producto> cbProducto = new ComboBox<>();
        cbProducto.setEditable(true);

        // Obtén la lista completa de productos de la base de datos
        ObservableList<Producto> listaProductos = FXCollections.observableArrayList();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id_producto, nombre, descripcion, id_categoria, precio, stock, sizes, colors, descuento, barcode " +
                             "FROM productos ORDER BY nombre"
             )) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Producto prod = new Producto();
                prod.setIdProducto(rs.getInt("id_producto"));
                prod.setNombre(rs.getString("nombre"));
                prod.setDescripcion(rs.getString("descripcion"));
                prod.setIdCategoria(rs.getInt("id_categoria"));
                prod.setPrecio(rs.getDouble("precio"));
                prod.setStock(rs.getInt("stock"));
                prod.setSizes(rs.getString("sizes"));
                prod.setColors(rs.getString("colors"));
                prod.setDescuento(rs.getDouble("descuento"));
                prod.setBarcode(rs.getString("barcode"));
                listaProductos.add(prod);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Crear una FilteredList para el autocompletar
        FilteredList<Producto> filteredProductos = new FilteredList<>(listaProductos, p -> true);
        cbProducto.setItems(filteredProductos);
        cbProducto.setConverter(new StringConverter<Producto>() {
            @Override
            public String toString(Producto object) {
                return object == null ? "" : object.getNombre();
            }
            @Override
            public Producto fromString(String string) {
                return null;
            }
        });

        // Listener que actualiza el predicado de la FilteredList según lo que se escriba
        cbProducto.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            filteredProductos.setPredicate(producto -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                return producto.getNombre().toLowerCase().contains(newVal.toLowerCase());
            });
            if (!cbProducto.isShowing()) {
                cbProducto.show();
            }
        });

        gridBusqueda.add(lblBuscar, 0, 0);
        gridBusqueda.add(cbProducto, 1, 0);
        vboxCont.getChildren().add(gridBusqueda);

        // --- Panel de edición (oculto inicialmente) ---
        GridPane gridEdicion = new GridPane();
        gridEdicion.setHgap(10);
        gridEdicion.setVgap(10);
        gridEdicion.setPadding(new Insets(10));
        gridEdicion.setVisible(false); // Se mostrará cuando se seleccione un producto

        // Campos del formulario de edición
        TextField tfNombre = new TextField();
        TextField tfDescripcion = new TextField();

        // ComboBox para Categorías
        ComboBox<Categoria> cbCategoria = new ComboBox<>();
        ObservableList<Categoria> listaCategorias = FXCollections.observableArrayList();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id_categoria, nombre FROM categorias ORDER BY nombre")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Categoria cat = new Categoria(rs.getInt("id_categoria"), rs.getString("nombre"));
                listaCategorias.add(cat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cbCategoria.setItems(listaCategorias);
        cbCategoria.setPromptText("Seleccione Categoría");
        cbCategoria.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria object) {
                return object == null ? "" : object.getNombre();
            }
            @Override
            public Categoria fromString(String string) {
                return null;
            }
        });

        TextField tfPrecio = new TextField();
        TextField tfStock = new TextField();
        TextField tfSizes = new TextField();
        TextField tfColors = new TextField();
        TextField tfDescuento = new TextField();
        TextField tfBarcode = new TextField();

        // Agregar etiquetas y campos al grid de edición
        gridEdicion.add(new Label("Nombre:"), 0, 0);
        gridEdicion.add(tfNombre, 1, 0);
        gridEdicion.add(new Label("Descripción:"), 0, 1);
        gridEdicion.add(tfDescripcion, 1, 1);
        gridEdicion.add(new Label("Categoría:"), 0, 2);
        gridEdicion.add(cbCategoria, 1, 2);
        gridEdicion.add(new Label("Precio:"), 0, 3);
        gridEdicion.add(tfPrecio, 1, 3);
        gridEdicion.add(new Label("Stock:"), 0, 4);
        gridEdicion.add(tfStock, 1, 4);
        gridEdicion.add(new Label("Sizes:"), 0, 5);
        gridEdicion.add(tfSizes, 1, 5);
        gridEdicion.add(new Label("Colors:"), 0, 6);
        gridEdicion.add(tfColors, 1, 6);
        gridEdicion.add(new Label("Descuento:"), 0, 7);
        gridEdicion.add(tfDescuento, 1, 7);
        gridEdicion.add(new Label("Código de Barras:"), 0, 8);
        gridEdicion.add(tfBarcode, 1, 8);

        vboxCont.getChildren().add(gridEdicion);

        // Configurar botones del diálogo: Guardar y Cancelar
        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().clear();
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(vboxCont);

        // Listener para cargar el formulario al seleccionar un producto
        cbProducto.valueProperty().addListener((obs, oldProducto, nuevoProducto) -> {
            if (nuevoProducto != null && (oldProducto == null || !nuevoProducto.equals(oldProducto))) {
                tfNombre.setText(nuevoProducto.getNombre());
                tfDescripcion.setText(nuevoProducto.getDescripcion());
                for (Categoria cat : listaCategorias) {
                    if (cat.getIdCategoria() == nuevoProducto.getIdCategoria()) {
                        cbCategoria.setValue(cat);
                        break;
                    }
                }
                tfPrecio.setText(String.valueOf(nuevoProducto.getPrecio()));
                tfStock.setText(String.valueOf(nuevoProducto.getStock()));
                tfSizes.setText(nuevoProducto.getSizes());
                tfColors.setText(nuevoProducto.getColors());
                tfDescuento.setText(String.valueOf(nuevoProducto.getDescuento()));
                tfBarcode.setText(nuevoProducto.getBarcode());
                gridEdicion.setVisible(true);

                // Aquí, bloqueamos la edición del ComboBox para no perder la selección
                cbProducto.setEditable(false);
            }
        });

        // Obtener el botón "Guardar" usando lookup y la misma instancia de ButtonType
        Node nodeSave = dialog.getDialogPane().lookupButton(btnGuardar);
        if (nodeSave instanceof Button) {
            Button saveBtn = (Button) nodeSave;
            saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Producto seleccionado = cbProducto.getValue();
                if (seleccionado != null) {
                    try {
                        String nuevoNombre = tfNombre.getText();
                        String nuevaDescripcion = tfDescripcion.getText();
                        Categoria nuevaCategoria = cbCategoria.getValue();
                        if (nuevaCategoria == null) {
                            mostrarMensaje("Debe seleccionar una categoría.", false);
                            event.consume();
                            return;
                        }
                        int idCategoria = nuevaCategoria.getIdCategoria();
                        double nuevoPrecio = Double.parseDouble(tfPrecio.getText());
                        int nuevoStock = Integer.parseInt(tfStock.getText());
                        String nuevosSizes = tfSizes.getText();
                        String nuevosColors = tfColors.getText();
                        double nuevoDescuento = Double.parseDouble(tfDescuento.getText());
                        String nuevoBarcode = tfBarcode.getText();

                        boolean actualizado = actualizarProducto(
                                seleccionado.getIdProducto(),
                                nuevoNombre,
                                nuevaDescripcion,
                                idCategoria,
                                nuevoPrecio,
                                nuevoStock,
                                nuevosSizes,
                                nuevosColors,
                                nuevoDescuento,
                                nuevoBarcode
                        );
                        if (actualizado) {
                            mostrarMensaje("Producto actualizado correctamente.", true);
                            dialog.close();
                        } else {
                            mostrarMensaje("Error al actualizar el producto.", false);
                            event.consume();
                        }
                    } catch (NumberFormatException ex) {
                        mostrarMensaje("Error en el formato de los campos numéricos.", false);
                        event.consume();
                    }
                }
            });
        } else {
            System.err.println("El botón Guardar es null; verifica la instancia de ButtonType.");
        }

        dialog.initOwner(getMainStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnShown(evt -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });
        dialog.showAndWait();
    }

    // Método para actualizar el producto en la base de datos
    private boolean actualizarProducto(int idProducto, String nombre, String descripcion, int idCategoria,
                                       double precio, int stock, String sizes, String colors,
                                       double descuento, String barcode) {
        boolean exito = false;
        String sql = "UPDATE productos SET nombre = ?, descripcion = ?, id_categoria = ?, precio = ?, stock = ?, sizes = ?, colors = ?, descuento = ?, barcode = ? WHERE id_producto = ?";

        System.out.println("=== DEBUG ACTUALIZARPRODUCTO ===");
        System.out.println("SQL: " + sql);
        System.out.println("ID Producto: " + idProducto);
        System.out.println("Nombre: " + nombre);
        System.out.println("Descripción: " + descripcion);
        System.out.println("ID Categoría: " + idCategoria);
        System.out.println("Precio: " + precio);
        System.out.println("Stock: " + stock);
        System.out.println("Sizes: " + sizes);
        System.out.println("Colors: " + colors);
        System.out.println("Descuento: " + descuento);
        System.out.println("Barcode: " + barcode);

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombre);
            stmt.setString(2, descripcion);
            stmt.setInt(3, idCategoria);
            stmt.setDouble(4, precio);
            stmt.setInt(5, stock);
            stmt.setString(6, sizes);
            stmt.setString(7, colors);
            stmt.setDouble(8, descuento);
            stmt.setString(9, barcode);
            stmt.setInt(10, idProducto);

            int filasAfectadas = stmt.executeUpdate();
            System.out.println("Filas afectadas: " + filasAfectadas);

            exito = filasAfectadas > 0;
        } catch (SQLException e) {
            System.err.println("=== ERROR SQL ===");
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }
        System.out.println("=== FIN DEBUG ACTUALIZARPRODUCTO ===");
        return exito;
    }


    // 3. Historial de Cliente con autocompletar y TableView de ventas
    @FXML
    private void onHistorialCliente() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Historial de Cliente");
        dialog.setHeaderText("Busque un cliente para ver su historial de ventas:");

        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(20));

        Label labelCliente = new Label("Cliente:");
        ComboBox<Cliente> cbCliente = new ComboBox<>();
        cbCliente.setEditable(true);

        // Obtener clientes
        ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id_cliente, nombre, direccion, correo, telefono " +
                             "FROM clientes ORDER BY nombre")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Cliente c = new Cliente();
                c.setIdCliente(rs.getInt("id_cliente"));
                c.setNombre(rs.getString("nombre"));
                c.setDireccion(rs.getString("direccion"));
                c.setCorreo(rs.getString("correo"));
                c.setTelefono(rs.getString("telefono"));
                listaClientes.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // FilteredList para autocompletar
        FilteredList<Cliente> filteredClientes = new FilteredList<>(listaClientes, c -> true);
        cbCliente.setItems(filteredClientes);
        cbCliente.setConverter(new StringConverter<Cliente>() {
            @Override
            public String toString(Cliente object) {
                return object == null ? "" : object.getNombre();
            }
            @Override
            public Cliente fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                return listaClientes.stream()
                        .filter(c -> c.getNombre().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        // Sincronización editor ↔ selección
        final boolean[] ignore = {false};

        cbCliente.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (ignore[0]) return;

            // 1) Actualizar filtro
            filteredClientes.setPredicate(cliente -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                return cliente.getNombre().toLowerCase().contains(newVal.toLowerCase());
            });

            // 2) Refrescar desplegable: ocultar y volver a mostrar si hay elementos
            Platform.runLater(() -> {
                if (filteredClientes.isEmpty()) {
                    cbCliente.hide();
                } else {
                    cbCliente.hide();
                    cbCliente.show();
                }
            });

            // 3) Selección exacta
            Cliente match = listaClientes.stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(newVal))
                    .findFirst()
                    .orElse(null);
            if (match != null && cbCliente.getValue() != match) {
                ignore[0] = true;
                cbCliente.setValue(match);
                ignore[0] = false;
            }
        });

        cbCliente.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (ignore[0]) return;
            if (newVal != null && !cbCliente.getEditor().getText().equals(newVal.getNombre())) {
                ignore[0] = true;
                cbCliente.getEditor().setText(newVal.getNombre());
                ignore[0] = false;
            }
        });

        vbox.getChildren().addAll(labelCliente, cbCliente);

        // TableView de ventas
        TableView<Object[]> ventasTable = new TableView<>();
        ventasTable.setPrefHeight(200);
        TableColumn<Object[], String> colVentaId = new TableColumn<>("ID Venta");
        colVentaId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[0].toString()));
        TableColumn<Object[], String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[1].toString()));
        TableColumn<Object[], String> colMetodo = new TableColumn<>("Método de Pago");
        colMetodo.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[2].toString()));
        TableColumn<Object[], String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[3].toString()));
        ventasTable.getColumns().addAll(colVentaId, colFecha, colMetodo, colTotal);
        vbox.getChildren().add(ventasTable);

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Al seleccionar cliente, cargar ventas
        cbCliente.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ObservableList<Object[]> ventasData = FXCollections.observableArrayList();
                try (Connection conn = DatabaseUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT id_venta, fecha, metodo_pago, total " +
                                     "FROM ventas WHERE id_cliente = ? ORDER BY fecha DESC")) {
                    stmt.setInt(1, newVal.getIdCliente());
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        Object[] row = {
                                rs.getInt("id_venta"),
                                rs.getTimestamp("fecha").toString(),
                                rs.getString("metodo_pago"),
                                rs.getBigDecimal("total").toString()
                        };
                        ventasData.add(row);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ventasTable.setItems(ventasData);
            } else {
                ventasTable.getItems().clear();
            }
        });

        dialog.initOwner(getMainStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnShown(evt -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });

        dialog.showAndWait();
    }


}