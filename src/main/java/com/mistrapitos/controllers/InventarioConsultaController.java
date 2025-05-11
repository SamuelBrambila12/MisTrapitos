package com.mistrapitos.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mistrapitos.models.Producto;
import com.mistrapitos.models.Categoria;
import com.mistrapitos.models.ResumenCategoria;
import com.mistrapitos.utils.DatabaseUtil;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class InventarioConsultaController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(InventarioConsultaController.class);

    // Tabla "Todos los productos"
    @FXML private TableView<Producto> productosTable;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colDescripcion;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, Double> colDescuento;
    @FXML private TableColumn<Producto, String> colBarcode;

    // Tabla "Poco Stock"
    @FXML private TableView<Producto> pocoStockTable;
    @FXML private TableColumn<Producto, Integer> colIdPoco;
    @FXML private TableColumn<Producto, String> colNombrePoco;
    @FXML private TableColumn<Producto, String> colCategoriaPoco;
    @FXML private TableColumn<Producto, Integer> colStockPoco;
    @FXML private TableColumn<Producto, Double> colPrecioPoco;

    // Tabla "Resumen por categoría"
    @FXML private TableView<ResumenCategoria> resumenTable;
    @FXML private TableColumn<ResumenCategoria, String> colCategoriaResumen;
    @FXML private TableColumn<ResumenCategoria, Integer> colTotalProductos;
    @FXML private TableColumn<ResumenCategoria, Integer> colStockTotal;
    @FXML private TableColumn<ResumenCategoria, BigDecimal> colValorInventario;
    @FXML private TableColumn<ResumenCategoria, Integer> colProductosBajoStock;

    // Filtros y estadísticas
    @FXML private ComboBox<Categoria> categoriasCombo;
    @FXML private Label totalProductosLabel;
    @FXML private Label totalStockLabel;
    @FXML private Label bajoStockLabel;
    @FXML private Label valorInventarioLabel;
    @FXML private Button cerrarBtn;

    // Listas para almacenar los datos
    private ObservableList<Producto> allProductos = FXCollections.observableArrayList();
    private FilteredList<Producto> filteredProductos;
    private ObservableList<Producto> pocoStockProductos = FXCollections.observableArrayList();
    private ObservableList<ResumenCategoria> resumenCategorias = FXCollections.observableArrayList();
    private ObservableList<Categoria> categorias = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupCategoriasCombo();
        loadData();
    }

    private void setupTableColumns() {
        // Configurar columnas para "Todos los productos"
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoriaNombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colDescuento.setCellValueFactory(new PropertyValueFactory<>("descuento"));
        colBarcode.setCellValueFactory(new PropertyValueFactory<>("barcode"));

        // Configurar columnas para "Poco Stock"
        colIdPoco.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colNombrePoco.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoriaPoco.setCellValueFactory(new PropertyValueFactory<>("categoriaNombre"));
        colStockPoco.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colPrecioPoco.setCellValueFactory(new PropertyValueFactory<>("precio"));

        // Configurar columnas para "Resumen por categoría"
        colCategoriaResumen.setCellValueFactory(new PropertyValueFactory<>("nombreCategoria"));
        colTotalProductos.setCellValueFactory(new PropertyValueFactory<>("totalProductos"));
        colStockTotal.setCellValueFactory(new PropertyValueFactory<>("stockTotal"));
        colValorInventario.setCellValueFactory(new PropertyValueFactory<>("valorInventario"));
        colProductosBajoStock.setCellValueFactory(new PropertyValueFactory<>("productosBajoStock"));

        // Destacar en rojo el stock en "Todos los productos" si es 5 o menor
        colStock.setCellFactory(column -> new TableCell<Producto, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(stock.toString());
                    setStyle(stock <= 5 ? "-fx-text-fill: red; -fx-font-weight: bold;" : "");
                }
            }
        });

        // Para "Poco Stock", siempre en rojo
        colStockPoco.setCellFactory(column -> new TableCell<Producto, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(stock.toString());
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void setupCategoriasCombo() {
        // Primera opción: "Todas las categorías"
        categorias.add(new Categoria(0, "Todas las categorías"));
        categoriasCombo.setItems(categorias);
        categoriasCombo.getSelectionModel().selectFirst();
    }

    private void loadData() {
        loadCategorias();
        loadProductos();
        loadPocoStockProductos();
        loadResumenCategorias();
        updateStats();
    }

    private void loadCategorias() {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id_categoria, nombre FROM categorias ORDER BY nombre")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                categorias.add(new Categoria(rs.getInt("id_categoria"), rs.getString("nombre")));
            }
            logger.info("Categorías cargadas: " + (categorias.size() - 1));
        } catch (SQLException e) {
            logger.error("Error al cargar categorías", e);
        }
    }

    private void loadProductos() {
        allProductos.clear();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT p.id_producto, p.nombre, p.descripcion, p.precio, p.stock, " +
                             "p.descuento, p.barcode, c.id_categoria, c.nombre as nombre_categoria " +
                             "FROM productos p " +
                             "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria " +
                             "ORDER BY p.nombre")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Producto producto = new Producto();
                producto.setIdProducto(rs.getInt("id_producto"));
                producto.setNombre(rs.getString("nombre"));
                producto.setDescripcion(rs.getString("descripcion"));
                producto.setPrecio(rs.getDouble("precio"));   // Usamos double
                producto.setStock(rs.getInt("stock"));
                producto.setDescuento(rs.getDouble("descuento")); // Usamos double
                producto.setBarcode(rs.getString("barcode"));
                producto.setIdCategoria(rs.getInt("id_categoria"));
                producto.setCategoriaNombre(rs.getString("nombre_categoria"));
                allProductos.add(producto);
            }

            filteredProductos = new FilteredList<>(allProductos, p -> true);
            productosTable.setItems(filteredProductos);
            logger.info("Productos cargados: " + allProductos.size());
        } catch (SQLException e) {
            logger.error("Error al cargar productos", e);
        }
    }

    private void loadPocoStockProductos() {
        pocoStockProductos.clear();
        // Filtramos aquellos con stock 5 o menor
        pocoStockProductos.addAll(allProductos.filtered(p -> p.getStock() <= 5));
        pocoStockTable.setItems(pocoStockProductos);
        logger.info("Productos con poco stock: " + pocoStockProductos.size());
    }

    private void loadResumenCategorias() {
        resumenCategorias.clear();
        Map<Integer, ResumenCategoria> resumenMap = new HashMap<>();
        // Categoría "Sin categoría" (ID 0)
        resumenMap.put(0, new ResumenCategoria(0, "Sin categoría", 0, 0, BigDecimal.ZERO, 0));
        // Agregar cada categoría (excluyendo la opción "Todas las categorías")
        for (Categoria cat : categorias) {
            if (cat.getIdCategoria() > 0) {
                resumenMap.put(cat.getIdCategoria(), new ResumenCategoria(
                        cat.getIdCategoria(), cat.getNombre(), 0, 0, BigDecimal.ZERO, 0
                ));
            }
        }
        // Acumular valores por categoría
        for (Producto producto : allProductos) {
            int idCat = producto.getIdCategoria() != 0 ? producto.getIdCategoria() : 0;
            ResumenCategoria resumen = resumenMap.get(idCat);
            if (resumen != null) {
                resumen.setTotalProductos(resumen.getTotalProductos() + 1);
                resumen.setStockTotal(resumen.getStockTotal() + producto.getStock());
                // Usar BigDecimal para el valor del inventario:
                BigDecimal valorProducto = BigDecimal.valueOf(producto.getPrecio() * producto.getStock());
                resumen.setValorInventario(resumen.getValorInventario().add(valorProducto));
                if (producto.getStock() <= 5) {
                    resumen.setProductosBajoStock(resumen.getProductosBajoStock() + 1);
                }
            }
        }
        resumenCategorias.addAll(resumenMap.values());
        resumenTable.setItems(resumenCategorias);
        logger.info("Resumen por categorías generado: " + resumenCategorias.size() + " categorías");
    }

    private void updateStats() {
        int totalProductos = allProductos.size();
        int totalStock = allProductos.stream().mapToInt(Producto::getStock).sum();
        int totalBajoStock = pocoStockProductos.size();
        double valorTotal = allProductos.stream()
                .mapToDouble(p -> p.getPrecio() * p.getStock())
                .sum();
        totalProductosLabel.setText(String.valueOf(totalProductos));
        totalStockLabel.setText(String.valueOf(totalStock));
        bajoStockLabel.setText(String.valueOf(totalBajoStock));
        valorInventarioLabel.setText("$" + valorTotal);
        logger.info("Estadísticas actualizadas - Total productos: " + totalProductos +
                ", Total stock: " + totalStock +
                ", Productos bajo stock: " + totalBajoStock +
                ", Valor total: $" + valorTotal);
    }

    @FXML
    private void onFiltrarPorCategoria() {
        Categoria selectedCategoria = categoriasCombo.getSelectionModel().getSelectedItem();
        if (selectedCategoria != null) {
            if (selectedCategoria.getIdCategoria() == 0) {
                filteredProductos.setPredicate(p -> true);
                logger.info("Filtro aplicado: Todas las categorías");
            } else {
                filteredProductos.setPredicate(p -> p.getIdCategoria() == selectedCategoria.getIdCategoria());
                logger.info("Filtro aplicado: Categoría " + selectedCategoria.getNombre());
            }
        }
    }

    @FXML
    private void onActualizar() {
        logger.info("Actualizando datos de inventario");
        loadData();
    }

    @FXML
    private void onCerrar() {
        logger.info("Cerrando ventana de consulta de inventario");
        Stage stage = (Stage) cerrarBtn.getScene().getWindow();
        stage.close();
    }
}
