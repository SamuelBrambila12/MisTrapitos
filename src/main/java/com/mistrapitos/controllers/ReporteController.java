
package com.mistrapitos.controllers;

import com.mistrapitos.models.*;
import com.mistrapitos.services.*;
import com.mistrapitos.utils.ReporteUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import java.io.File;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ReporteController {

    @FXML private ComboBox<String> tipoReporteCombo;
    @FXML private ComboBox<String> formatoCombo;
    @FXML private DatePicker mesPicker;
    @FXML private DatePicker fechaInicioPicker;
    @FXML private DatePicker fechaFinPicker;
    @FXML private ComboBox<Cliente> clienteCombo;
    @FXML private ComboBox<Proveedor> proveedorCombo;
    @FXML private Label mensajeLabel;
    @FXML private TableView<?> tablaReporte;
    @FXML private ComboBox<String> ciudadCombo;
    @FXML private CheckBox todasCiudadesCheck;
    private final VentaService ventaService         = new VentaService();
    private final ProductoService productoService   = new ProductoService();
    private final ProveedorService proveedorService = new ProveedorService();
    private final ClienteService clienteService     = new ClienteService();

    // Método utilitario para obtener el Stage principal
    private Stage getMainStage() {
        return (Stage) tablaReporte.getScene().getWindow();
    }

    @FXML
    public void initialize() {
        clienteCombo.setConverter(new StringConverter<Cliente>() {
            @Override
            public String toString(Cliente cli) {
                return (cli == null) ? "" : cli.getNombre();
            }
            @Override
            public Cliente fromString(String s) {
                return null; // no lo necesitamos
            }
        });
        proveedorCombo.setConverter(new StringConverter<Proveedor>() {
            @Override
            public String toString(Proveedor pr) {
                return (pr == null) ? "" : pr.getNombre();
            }
            @Override
            public Proveedor fromString(String s) {
                return null;
            }
        });
        // Poblar tipos de reporte
        tipoReporteCombo.setItems(FXCollections.observableArrayList(
                "Ventas Diarias",
                "Ventas Semanales",
                "Ventas Mensuales",
                "Ventas por Categoría Mensual",
                "Ventas Últimos 3 Días",
                "Ventas por Ciudad",
                "Inventario",
                "Productos Más Vendidos",
                "Productos Más Vendidos en Mes",
                "Métodos de Pago",
                "Producto con Más Stock",
                "Productos de Proveedor",
                "Comprados Más de una Vez",
                "Productos no Vendidos 3 Meses"
        ));
        tipoReporteCombo.getSelectionModel().selectFirst();

        // Formatos
        formatoCombo.setItems(FXCollections.observableArrayList("Excel","PDF"));
        formatoCombo.getSelectionModel().selectFirst();

        // DatePickers
        mesPicker.setValue(LocalDate.now().withDayOfMonth(1));
        mesPicker.setPromptText("MM/yyyy");
        fechaInicioPicker.setValue(LocalDate.now().minusDays(2));
        fechaInicioPicker.setPromptText("Fecha Inicio");
        fechaFinPicker.setValue(LocalDate.now());
        fechaFinPicker.setPromptText("Fecha Fin");

        // Combo de clientes y proveedores
        clienteCombo.setItems(FXCollections.observableArrayList(clienteService.obtenerTodos()));
        proveedorCombo.setItems(FXCollections.observableArrayList(proveedorService.obtenerTodos()));

        // Listeners para ajustar visibilidad y recargar
        tipoReporteCombo.setOnAction(e -> ajustarControles());
        mesPicker.setOnAction(e -> cargarDatosReporte());
        fechaInicioPicker.setOnAction(e -> cargarDatosReporte());
        fechaFinPicker.setOnAction(e -> cargarDatosReporte());
        clienteCombo.setOnAction(e -> cargarDatosReporte());
        proveedorCombo.setOnAction(e -> cargarDatosReporte());
        // Cargar las ciudades disponibles
        List<String> ciudades = clienteService.obtenerCiudadesUnicas();
        ciudadCombo.setItems(FXCollections.observableArrayList(ciudades));
        ciudadCombo.getSelectionModel().selectFirst();

        // Configurar el listener del checkbox
        todasCiudadesCheck.setOnAction(e -> {
            ciudadCombo.setDisable(todasCiudadesCheck.isSelected());
            cargarDatosReporte();
        });

        ciudadCombo.setOnAction(e -> cargarDatosReporte());
        ajustarControles();
    }

    /** Muestra/oculta controles según el tipo de reporte */
    private void ajustarControles() {
        String tipo = tipoReporteCombo.getValue();

        boolean usaMes = tipo.equals("Ventas Mensuales")
                || tipo.equals("Productos Más Vendidos en Mes")
                || tipo.equals("Métodos de Pago")
                || tipo.equals("Ventas por Categoría Mensual");
        boolean usaRangoFechas = tipo.equals("Ventas Personalizadas")
                || tipo.equals("Ventas Últimos 3 Días");
        boolean usaCliente     = tipo.equals("Comprados Más de una Vez");
        boolean usaProveedor   = tipo.equals("Productos de Proveedor");
        boolean usaCiudad = tipo.equals("Ventas por Ciudad");

        ciudadCombo.setVisible(usaCiudad);
        ciudadCombo.setDisable(!usaCiudad || todasCiudadesCheck.isSelected());
        todasCiudadesCheck.setVisible(usaCiudad);
        todasCiudadesCheck.setDisable(!usaCiudad);
        mesPicker.setVisible(usaMes);
        mesPicker.setDisable(!usaMes);

        fechaInicioPicker.setVisible(usaRangoFechas);
        fechaInicioPicker.setDisable(!usaRangoFechas);
        fechaFinPicker.setVisible(usaRangoFechas);
        fechaFinPicker.setDisable(!usaRangoFechas);

        clienteCombo.setVisible(usaCliente);
        clienteCombo.setDisable(!usaCliente);

        proveedorCombo.setVisible(usaProveedor);
        proveedorCombo.setDisable(!usaProveedor);

        cargarDatosReporte();
    }

    /** Exporta y guarda el reporte tras actualizar la tabla */
    @FXML
    private void onGenerarReporte() {
        // 1) Aseguramos que la tabla esté actualizada
        cargarDatosReporte();

        // 2) Leemos el tipo de reporte y el formato
        String tipo    = tipoReporteCombo.getValue();
        String formato = formatoCombo.getValue();

        // 3) Base del nombre de archivo
        String base = "reporte_" + tipo.replace(' ', '_').toLowerCase();

        // 4) Sufijos específicos
        if (tipo.equals("Ventas Diarias")) {
            LocalDate d = LocalDate.now();
            String sufijo = d.format(
                    DateTimeFormatter.ofPattern("dd_MMMM_yyyy", new Locale("es","ES"))
            ).toLowerCase();
            base += "_" + sufijo;
        }
        else if (tipo.equals("Ventas por Ciudad")) {
            if (todasCiudadesCheck.isSelected()) {
                base += "_todas";
            } else {
                String ciudad = ciudadCombo.getValue() != null
                        ? ciudadCombo.getValue().trim().replaceAll("\\s+","_").toLowerCase()
                        : "sin_ciudad";
                base += "_" + ciudad;
            }
        }
        else if (tipo.equals("Productos de Proveedor")) {
            String prov = proveedorCombo.getValue() != null
                    ? proveedorCombo.getValue().getNombre().trim().replaceAll("\\s+","_").toLowerCase()
                    : "sin_proveedor";
            base += "_" + prov;
        }
        else if (tipo.equals("Comprados Más de una Vez")) {
            Cliente cli = clienteCombo.getValue();
            String nombreCli = (cli != null)
                    ? cli.getNombre().trim().replaceAll("\\s+","_").toLowerCase()
                    : "sin_cliente";
            base += "_" + nombreCli;
        }
        else if (tipo.equals("Métodos de Pago")
                || tipo.equals("Productos Más Vendidos en Mes")
                || tipo.equals("Ventas Mensuales")) {

            LocalDate m = mesPicker.getValue() != null
                    ? mesPicker.getValue().withDayOfMonth(1)
                    : LocalDate.now().withDayOfMonth(1);
            String sufijoMes = m.format(
                    DateTimeFormatter.ofPattern("MMMM_yyyy", new Locale("es","ES"))
            ).toLowerCase();
            base += "_" + sufijoMes;
        }
        else if (tipo.equals("Ventas por Categoría Mensual")) {
            LocalDate m = mesPicker.getValue() != null
                    ? mesPicker.getValue().withDayOfMonth(1)
                    : LocalDate.now().withDayOfMonth(1);
            String sufijoMes = m.format(
                    DateTimeFormatter.ofPattern("MMMM_yyyy", new Locale("es","ES"))
            ).toLowerCase();
            base += "_" + sufijoMes;
        }

        // 5) FileChooser
        String ext = formato.equals("Excel") ? "xlsx" : "pdf";
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar Reporte");
        chooser.setInitialFileName(base + "." + ext);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(formato + " (*." + ext + ")", "*." + ext)
        );
        // --- Asegura que el FileChooser esté sobre la pantalla completa ---
        File file = chooser.showSaveDialog(getMainStage());
        if (file == null) return;

        // 6) Recogemos datos y disparamos ReporteUtil
        boolean exito;
        ObservableList<?> items = tablaReporte.getItems();
        switch (tipo) {
            case "Ventas Diarias":
            case "Ventas Semanales":
            case "Ventas Mensuales":
            case "Ventas Últimos 3 Días": {
                List<Venta> vs = items.stream().map(i -> (Venta) i).collect(Collectors.toList());
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteVentasExcel(vs, file.getAbsolutePath())
                        : ReporteUtil.generarReporteVentasPDF(vs, file.getAbsolutePath());
                break;
            }
            case "Inventario": {
                List<Producto> ps = items.stream().map(i -> (Producto) i).collect(Collectors.toList());
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteInventarioExcel(ps, file.getAbsolutePath())
                        : ReporteUtil.generarReporteInventarioPDF(ps, file.getAbsolutePath());
                break;
            }
            case "Productos Más Vendidos":
            case "Productos Más Vendidos en Mes": {
                List<ProductoVentaResumen> pr = items.stream().map(i -> (ProductoVentaResumen) i).collect(Collectors.toList());
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteProductosMasVendidosExcel(pr, file.getAbsolutePath())
                        : ReporteUtil.generarReporteProductosMasVendidosPDF(pr, file.getAbsolutePath());
                break;
            }
            case "Métodos de Pago": {
                List<MetodoPagoResumen> mp = items.stream().map(i -> (MetodoPagoResumen) i).collect(Collectors.toList());
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteMetodosPagoExcel(mp, file.getAbsolutePath())
                        : ReporteUtil.generarReporteMetodosPagoPDF(mp, file.getAbsolutePath());
                break;
            }
            case "Producto con Más Stock": {
                List<Producto> mayor = items.stream().map(i -> (Producto) i).collect(Collectors.toList());
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteProductoMayorStockExcel(mayor, file.getAbsolutePath())
                        : ReporteUtil.generarReporteProductoMayorStockPDF(mayor, file.getAbsolutePath());
                break;
            }
            case "Productos de Proveedor": {
                List<Producto> prov = items.stream().map(i -> (Producto) i).collect(Collectors.toList());
                String nombreProv = proveedorCombo.getValue().getNombre();
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteProductosPorProveedorExcel(nombreProv, prov, file.getAbsolutePath())
                        : ReporteUtil.generarReporteProductosPorProveedorPDF(nombreProv, prov, file.getAbsolutePath());
                break;
            }
            case "Comprados Más de una Vez": {
                List<ReporteController.ProductoRepetido> rep = items.stream()
                        .map(i -> (ReporteController.ProductoRepetido) i).collect(Collectors.toList());
                String nombreCli = clienteCombo.getValue().getNombre();
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteCompradosMasDeUnaVezExcel(nombreCli, rep, file.getAbsolutePath())
                        : ReporteUtil.generarReporteCompradosMasDeUnaVezPDF(nombreCli, rep, file.getAbsolutePath());
                break;
            }
            case "Ventas por Ciudad": {
                List<VentaPorCiudad> ventasCiudad = items.stream()
                        .map(i -> (VentaPorCiudad) i).collect(Collectors.toList());
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteVentasPorCiudadExcel(ventasCiudad, file.getAbsolutePath())
                        : ReporteUtil.generarReporteVentasPorCiudadPDF(ventasCiudad, file.getAbsolutePath());
                break;
            }
            case "Ventas por Categoría Mensual": {
                List<CategoriaVentaResumen> data = items.stream()
                        .map(i -> (CategoriaVentaResumen) i).collect(Collectors.toList());
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteVentasPorCategoriaExcel(data, file.getAbsolutePath())
                        : ReporteUtil.generarReporteVentasPorCategoriaPDF(data, file.getAbsolutePath());
                break;
            }
            case "Productos no Vendidos 3 Meses": {
                List<Producto> nov = items.stream().map(i -> (Producto) i).collect(Collectors.toList());
                exito = formato.equals("Excel")
                        ? ReporteUtil.generarReporteProductosNoVendidos3MesesExcel(nov, file.getAbsolutePath())
                        : ReporteUtil.generarReporteProductosNoVendidos3MesesPDF(nov, file.getAbsolutePath());
                break;
            }
            default:
                exito = true;
        }

        // 7) Mensaje final
        mostrarAlerta(exito ? "Reporte generado correctamente." : "Error al generar el reporte.", exito ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
    }


    @FXML
    public Callback<TableView<Producto>, TableRow<Producto>> stockRowFactory() {
        return table -> new TableRow<Producto>() {
            @Override
            protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    int idx   = getIndex();
                    int total = table.getItems().size();
                    if (idx < 5) {
                        setStyle("-fx-background-color: lightgreen;");
                    } else if (idx >= total - 3) {
                        setStyle("-fx-background-color: lightcoral;");
                    } else {
                        setStyle("");
                    }
                }
            }
        };
    }

    /** Actualiza la tabla según el reporte seleccionado */
    private void cargarDatosReporte() {
        tablaReporte.getColumns().clear();
        tablaReporte.getItems().clear();
        String tipo = tipoReporteCombo.getValue();
        switch (tipo) {
            case "Ventas Diarias":           cargarVentasDiarias();      break;
            case "Ventas Semanales":         cargarVentasSemanales();    break;
            case "Ventas Mensuales":         cargarVentasMensuales();    break;
            case "Ventas por Categoría Mensual": cargarVentasPorCategoriaMes(); break;
            case "Ventas Últimos 3 Días":    cargarVentasUltimos3Dias(); break;
            case "Ventas por Ciudad":        cargarVentasPorCiudad();    break;
            case "Inventario":               cargarInventario();         break;
            case "Productos Más Vendidos":   cargarProductosMasVendidos();break;
            case "Productos Más Vendidos en Mes": cargarProductosMasVendidosMes(); break;
            case "Métodos de Pago":          cargarMetodosPago();        break;
            case "Producto con Más Stock":   cargarProductoMayorStock(); break;
            case "Productos de Proveedor":   cargarProductosPorProveedor(); break;
            case "Comprados Más de una Vez": cargarCompradosMasDeUnaVez(); break;
            case "Productos no Vendidos 3 Meses": cargarProductosNoVendidos3Meses(); break;
        }
    }

    private void cargarVentasDiarias() {
        ObservableList<Venta> obs = FXCollections.observableArrayList(
                ventaService.buscarPorFecha(LocalDate.now())
        );
        TableColumn<Venta,Integer> c1 = new TableColumn<>("ID");
        c1.setCellValueFactory(c -> c.getValue().idVentaProperty().asObject());
        TableColumn<Venta,String> c2 = new TableColumn<>("Cliente");
        c2.setCellValueFactory(c -> c.getValue().clienteNombreProperty());
        TableColumn<Venta,String> c3 = new TableColumn<>("Fecha");
        c3.setCellValueFactory(c -> new SimpleObjectProperty<>(
                c.getValue().getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm a"))
        ));
        TableColumn<Venta,Double> c4 = new TableColumn<>("Total");
        c4.setCellValueFactory(c -> c.getValue().totalProperty().asObject());
        mostrarTabla(obs, Arrays.asList(c1,c2,c3,c4));
    }
    private void cargarVentasPorCategoriaMes() {
        // 1) Determina mes de mesPicker (o actual)
        LocalDate m = (mesPicker.getValue() != null)
                ? mesPicker.getValue().withDayOfMonth(1)
                : LocalDate.now().withDayOfMonth(1);
        YearMonth ym = YearMonth.from(m);
        LocalDate inicio = ym.atDay(1);
        LocalDate fin    = ym.atEndOfMonth();

        // 2) Pide al servicio el resumen por categoría
        List<CategoriaVentaResumen> lista =
                ventaService.obtenerVentasPorCategoria(inicio, fin);

        // 3) Llena el TableView
        ObservableList<CategoriaVentaResumen> obs =
                FXCollections.observableArrayList(lista);

        TableColumn<CategoriaVentaResumen,String> colCat = new TableColumn<>("Categoría");
        colCat.setCellValueFactory(c ->
                c.getValue().categoriaProperty());

        TableColumn<CategoriaVentaResumen,Double> colTot = new TableColumn<>("Total Vendido");
        colTot.setCellValueFactory(c ->
                c.getValue().totalProperty().asObject());

        mostrarTabla(obs, Arrays.asList(colCat, colTot));
    }

    private void cargarVentasSemanales() {
        LocalDate hoy = LocalDate.now();
        LocalDate ini = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
        LocalDate fin = ini.plusDays(6);
        cargarVentasRango(ini, fin);
    }
    private void cargarVentasPorCiudad() {
        List<VentaPorCiudad> ventas;
        if (todasCiudadesCheck.isSelected()) {
            ventas = ventaService.obtenerVentasPorCiudad();
        } else {
            String ciudad = ciudadCombo.getValue();
            if (ciudad == null || ciudad.isEmpty()) return;
            ventas = ventaService.obtenerVentasPorCiudad(ciudad);
        }

        ObservableList<VentaPorCiudad> obs = FXCollections.observableArrayList(ventas);

        TableColumn<VentaPorCiudad, String> c1 = new TableColumn<>("Ciudad");
        c1.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCiudad()));

        TableColumn<VentaPorCiudad, Integer> c2 = new TableColumn<>("Cantidad Ventas");
        c2.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCantidadVentas()));

        TableColumn<VentaPorCiudad, Double> c3 = new TableColumn<>("Total Vendido");
        c3.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotalVendido()));

        // Nueva columna de productos
        TableColumn<VentaPorCiudad, String> c4 = new TableColumn<>("Productos");
        c4.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getProductos()));
        c4.setPrefWidth(600);  // opcional para que no quede muy estrecha

        mostrarTabla(obs, Arrays.asList(c1, c2, c3, c4));
    }


    private void cargarVentasMensuales() {
        LocalDate m = mesPicker.getValue() != null
                ? mesPicker.getValue().withDayOfMonth(1)
                : LocalDate.now().withDayOfMonth(1);
        YearMonth ym = YearMonth.from(m);
        cargarVentasRango(ym.atDay(1), ym.atEndOfMonth());
    }

    private void cargarVentasUltimos3Dias() {
        LocalDate fin    = LocalDate.now();        // e.g. 2025-04-19
        LocalDate inicio = fin.minusDays(2);       // e.g. 2025-04-17

        // Mostrar el rango en los pickers
        fechaInicioPicker.setValue(inicio);
        fechaFinPicker.setValue(fin);

        cargarVentasRango(inicio, fin);
    }

    /** Genérico para cualquier rango */
    private void cargarVentasRango(LocalDate start, LocalDate end) {
        ObservableList<Venta> obs = FXCollections.observableArrayList(
                ventaService.buscarPorRangoFechas(start, end)
        );

        // Columna ID
        TableColumn<Venta,Integer> c1 = new TableColumn<>("ID");
        c1.setCellValueFactory(c -> c.getValue().idVentaProperty().asObject());

        // **Nueva columna Cliente**
        TableColumn<Venta,String> c2 = new TableColumn<>("Cliente");
        c2.setCellValueFactory(c -> c.getValue().clienteNombreProperty());

        // Columna Fecha
        TableColumn<Venta,String> c3 = new TableColumn<>("Fecha");
        c3.setCellValueFactory(c ->
                new SimpleObjectProperty<>(
                        c.getValue().getFecha()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm a"))
                )
        );

        // Columna Total
        TableColumn<Venta,Double> c4 = new TableColumn<>("Total");
        c4.setCellValueFactory(c -> c.getValue().totalProperty().asObject());

        // Ahora pasamos 4 columnas en lugar de 3
        mostrarTabla(obs, Arrays.asList(c1, c2, c3, c4));
    }

    public static class VentaPorCiudad {
        private final String ciudad;
        private final int    cantidadVentas;
        private final double totalVendido;
        private final String productos;

        public VentaPorCiudad(String ciudad, int cantidadVentas, double totalVendido, String productos) {
            this.ciudad         = ciudad;
            this.cantidadVentas = cantidadVentas;
            this.totalVendido   = totalVendido;
            this.productos      = productos;
        }
        public String getCiudad()        { return ciudad;        }
        public int    getCantidadVentas(){ return cantidadVentas;}
        public double getTotalVendido()  { return totalVendido;  }
        public String getProductos()     { return productos;     }
    }


    private void cargarInventario() {
        ObservableList<Producto> obs = FXCollections.observableArrayList(
                productoService.obtenerTodos()
        );
        TableColumn<Producto,Integer> c1 = new TableColumn<>("ID");
        c1.setCellValueFactory(c -> c.getValue().idProductoProperty().asObject());
        TableColumn<Producto,String> c2 = new TableColumn<>("Nombre");
        c2.setCellValueFactory(c -> c.getValue().nombreProperty());
        TableColumn<Producto,String> c3 = new TableColumn<>("Categoría");
        c3.setCellValueFactory(c -> c.getValue().categoriaNombreProperty());
        TableColumn<Producto,Double> c4 = new TableColumn<>("Precio");
        c4.setCellValueFactory(c -> c.getValue().precioProperty().asObject());
        TableColumn<Producto,Integer> c5 = new TableColumn<>("Stock");
        c5.setCellValueFactory(c -> c.getValue().stockProperty().asObject());
        TableColumn<Producto,String> c6 = new TableColumn<>("Tallas");
        c6.setCellValueFactory(c -> c.getValue().sizesProperty());
        TableColumn<Producto,String> c7 = new TableColumn<>("Colores");
        c7.setCellValueFactory(c -> c.getValue().colorsProperty());
        TableColumn<Producto,Double> c8 = new TableColumn<>("Descuento");
        c8.setCellValueFactory(c -> c.getValue().descuentoProperty().asObject());
        mostrarTabla(obs, Arrays.asList(c1,c2,c3,c4,c5,c6,c7,c8));
    }

    private void cargarProductosMasVendidos() {
        ObservableList<ProductoVentaResumen> obs = FXCollections.observableArrayList(
                ventaService.obtenerProductosMasVendidos()
        );
        TableColumn<ProductoVentaResumen,String> c1 = new TableColumn<>("Nombre");
        c1.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNombre()));
        TableColumn<ProductoVentaResumen,Integer> c2 = new TableColumn<>("Cantidad");
        c2.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCantidadVendida()));
        TableColumn<ProductoVentaResumen,Double> c3 = new TableColumn<>("Total");
        c3.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotalVendido()));
        mostrarTabla(obs, Arrays.asList(c1,c2,c3));
    }

    private void cargarProductosMasVendidosMes() {
        LocalDate m = mesPicker.getValue() != null
                ? mesPicker.getValue()
                : LocalDate.now().withDayOfMonth(1);
        YearMonth ym = YearMonth.from(m);
        List<ProductoVentaResumen> lista =
                new ReporteUtil().obtenerProductosMasVendidos(ym.atDay(1), ym.atEndOfMonth());

        ObservableList<ProductoVentaResumen> obs = FXCollections.observableArrayList(lista);

        TableColumn<ProductoVentaResumen,String> c1 = new TableColumn<>("Nombre");
        c1.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNombre()));

        TableColumn<ProductoVentaResumen,Integer> c2 = new TableColumn<>("Cantidad");
        c2.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCantidadVendida()));

        // Nueva columna Total
        TableColumn<ProductoVentaResumen,Double> c3 = new TableColumn<>("Total");
        c3.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotalVendido()));

        mostrarTabla(obs, Arrays.asList(c1,c2,c3));
    }
    private void cargarMetodosPago() {
        // Obtén el mes seleccionado, o usa el mes actual por defecto
        LocalDate m = mesPicker.getValue() != null
                ? mesPicker.getValue().withDayOfMonth(1)
                : LocalDate.now().withDayOfMonth(1);
        YearMonth ym = YearMonth.from(m);
        LocalDate inicio = ym.atDay(1);
        LocalDate fin    = ym.atEndOfMonth();

        // Llama al servicio con ese rango
        List<MetodoPagoResumen> lista = ventaService.obtenerMetodosPagoMasUtilizados(inicio, fin);
        ObservableList<MetodoPagoResumen> obs = FXCollections.observableArrayList(lista);

        // Columnas
        TableColumn<MetodoPagoResumen,String> c1 = new TableColumn<>("Método");
        c1.setCellValueFactory(c -> c.getValue().metodoPagoProperty());
        TableColumn<MetodoPagoResumen,Integer> c2 = new TableColumn<>("Veces");
        c2.setCellValueFactory(c -> c.getValue().vecesProperty().asObject());

        mostrarTabla(obs, Arrays.asList(c1, c2));
    }


    private void cargarProductoMayorStock() {
        List<Producto> todos = productoService.obtenerTodos();
        // Orden descendente por stock
        List<Producto> ordenados = todos.stream()
                .sorted(Comparator.comparingInt(Producto::getStock).reversed())
                .collect(Collectors.toList());

        ObservableList<Producto> obs = FXCollections.observableArrayList(ordenados);

        // Columna Nombre
        TableColumn<Producto,String> colNom = new TableColumn<>("Nombre");
        colNom.setCellValueFactory(c -> c.getValue().nombreProperty());

        // Columna Stock con cellFactory para colorear el texto
        TableColumn<Producto,Integer> colSto = new TableColumn<>("Stock");
        colSto.setCellValueFactory(c -> c.getValue().stockProperty().asObject());
        colSto.setCellFactory(column -> new TableCell<Producto,Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(item.toString());
                    int idx   = getIndex();
                    int total = getTableView().getItems().size();
                    if (idx < 3) {
                        // Top 3 en verde
                        setTextFill(Color.GREEN);
                    } else if (idx >= total - 3) {
                        // Últimos 3 en rojo
                        setTextFill(Color.RED);
                    } else {
                        // Resto en color por defecto
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });

        @SuppressWarnings("unchecked")
        TableView<Producto> tv = (TableView<Producto>) tablaReporte;
        tv.getColumns().setAll(colNom, colSto);
        tv.setItems(obs);
    }

    private void cargarCompradosMasDeUnaVez() {
        Cliente cli = clienteCombo.getValue();
        if (cli == null) return;

        // 1) Detalles de venta del cliente
        List<DetalleVenta> dets = ventaService.obtenerDetallesVenta(cli.getIdCliente());
        // 2) Agrupamos por producto y sumamos cantidades
        Map<Integer,Integer> counts = dets.stream()
                .collect(Collectors.groupingBy(
                        DetalleVenta::getIdProducto,
                        Collectors.summingInt(DetalleVenta::getCantidad)
                ));
        // 3) Filtramos >=2 y resolvemos nombre
        List<ProductoRepetido> rpt = counts.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .flatMap(e ->
                        productoService.obtenerPorId(e.getKey()).stream()
                                .map(p -> new ProductoRepetido(p.getNombre(), e.getValue()))
                )
                .collect(Collectors.toList());

        ObservableList<ProductoRepetido> data = FXCollections.observableArrayList(rpt);
        TableColumn<ProductoRepetido,String> colProd = new TableColumn<>("Producto");
        colProd.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNombre()));
        TableColumn<ProductoRepetido,Integer> colVeces = new TableColumn<>("Veces");
        colVeces.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getVeces()));

        @SuppressWarnings("unchecked")
        TableView<ProductoRepetido> tv = (TableView<ProductoRepetido>) tablaReporte;
        tv.getColumns().setAll(colProd, colVeces);
        tv.setItems(data);
    }

    public static class ProductoRepetido {
        private final String nombre;
        private final int    veces;
        public ProductoRepetido(String nombre,int veces) {
            this.nombre = nombre;
            this.veces   = veces;
        }
        public String getNombre(){ return nombre; }
        public int    getVeces() { return veces;   }
    }

    private void cargarProductosPorProveedor() {
        Proveedor pr = proveedorCombo.getValue();
        if (pr == null) return;

        String txt = pr.getProductosVendidos();
        if (txt == null || txt.isBlank()) {
            mostrarAlerta("No hay productos para este proveedor.", Alert.AlertType.INFORMATION);
            return;
        }

        Set<String> names = Arrays.stream(txt.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        List<Producto> list = productoService.obtenerTodos().stream()
                .filter(p -> names.contains(p.getNombre()))
                .collect(Collectors.toList());

        ObservableList<Producto> data = FXCollections.observableArrayList(list);
        TableColumn<Producto,String> colNom = new TableColumn<>("Producto");
        colNom.setCellValueFactory(c -> c.getValue().nombreProperty());
        TableColumn<Producto,Integer> colSto = new TableColumn<>("Stock");
        colSto.setCellValueFactory(c -> c.getValue().stockProperty().asObject());

        @SuppressWarnings("unchecked")
        TableView<Producto> tv = (TableView<Producto>) tablaReporte;
        tv.getColumns().setAll(colNom, colSto);
        tv.setItems(data);
    }

    private void cargarProductosNoVendidos3Meses() {
        LocalDate now = LocalDate.now();
        Set<String> soldNames = new ReporteUtil()
                .obtenerProductosMasVendidos(now.minusMonths(3), now)
                .stream()
                .map(ProductoVentaResumen::getNombre)
                .collect(Collectors.toSet());
        List<Producto> all = productoService.obtenerTodos();
        ObservableList<Producto> obs = FXCollections.observableArrayList(
                all.stream()
                        .filter(p -> !soldNames.contains(p.getNombre()))
                        .collect(Collectors.toList())
        );
        TableColumn<Producto,String> c1 = new TableColumn<>("Nombre");
        c1.setCellValueFactory(c -> c.getValue().nombreProperty());
        mostrarTabla(obs, Collections.singletonList(c1));
    }

    // Método para mostrar Alert sobre pantalla completa
    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo, mensaje, ButtonType.OK);
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

    // Si necesitas mostrar un Dialog personalizado, usa este método
    private <T> Optional<T> mostrarDialogoPersonalizado(Dialog<T> dialog) {
        dialog.initOwner(getMainStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnShown(evt -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/logo.jpg"))
            );
        });
        return dialog.showAndWait();
    }

    private void mostrarMensaje(boolean exito) {
        mensajeLabel.setText(exito ? "Reporte generado correctamente." : "Error al generar el reporte.");
        mensajeLabel.setStyle(exito ? "-fx-text-fill: #00b894" : "-fx-text-fill: #d63031");
        mensajeLabel.setVisible(true);
        new Thread(() -> {
            try { Thread.sleep(3000); }
            catch (InterruptedException ignored) {}
            Platform.runLater(() -> mensajeLabel.setVisible(false));
        }).start();
    }

    @SuppressWarnings("unchecked")
    private <T> void mostrarTabla(ObservableList<T> data, List<TableColumn<T, ?>> cols) {
        TableView<T> tv = (TableView<T>) tablaReporte;
        tv.getColumns().setAll(cols);
        tv.setItems(data);
    }
}
