package com.mistrapitos.services;

import com.mistrapitos.dao.ClienteDao;
import com.mistrapitos.dao.ProductoDao;
import com.mistrapitos.dao.VentaDao;
import com.mistrapitos.dao.DetalleVentaDao;
import com.mistrapitos.models.Cliente;
import com.mistrapitos.models.DetalleVenta;
import com.mistrapitos.models.Producto;
import com.mistrapitos.models.CategoriaVentaResumen;
import com.mistrapitos.utils.ReporteUtil;
import com.mistrapitos.utils.DatabaseUtil;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import com.mistrapitos.controllers.ReporteController;
import com.mistrapitos.models.Venta;
import com.mistrapitos.models.ProductoVentaResumen;
import com.mistrapitos.models.ProductoEnCarrito;
import com.mistrapitos.models.Usuario;
import org.slf4j.Logger;
import java.util.Collections;
import com.mistrapitos.models.MetodoPagoResumen;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de ventas.
 */
public class VentaService {
    
    private static final Logger logger = LoggerFactory.getLogger(VentaService.class);
    private final VentaDao ventaDao;
    private final ClienteDao clienteDao;
    private final ProductoDao productoDao;
    private final DetalleVentaDao detalleVentaDao;
    
    /**
     * Constructor.
     */
    public VentaService() {
        this.ventaDao = new VentaDao();
        this.clienteDao = new ClienteDao();
        this.productoDao = new ProductoDao();
        this.detalleVentaDao = new DetalleVentaDao();
    }
    
    /**
     * Obtiene todas las ventas.
     * @return Lista de ventas
     */
    public List<Venta> obtenerTodas() {
        return ventaDao.findAll();
    }

    /**
     * Obtiene los detalles de una venta por su ID.
     * @param idVenta ID de la venta
     * @return Lista de detalles de la venta
     */
    public List<DetalleVenta> obtenerDetallesVenta(int idVenta) {
        return detalleVentaDao.findByVenta(idVenta);
    }
    /**
     * Obtiene una venta por su ID.
     * @param id ID de la venta
     * @return Venta encontrada o vacío si no existe
     */
    public Optional<Venta> obtenerPorId(int id) {
        return ventaDao.findById(id);
    }
    /**
     * Obtiene la lista de productos más vendidos en total histórico (todas las ventas).
     * @return Lista de productos más vendidos, ordenados por cantidad vendida descendente
     */
    public List<ProductoVentaResumen> obtenerProductosMasVendidos() {
        return ventaDao.obtenerProductosMasVendidos();
    }

    public List<CategoriaVentaResumen> obtenerVentasPorCategoria(LocalDate inicio, LocalDate fin) {
        return new ReporteUtil()
                .obtenerVentasPorCategoria(inicio, fin);
    }

    /**
     * Busca ventas por cliente.
     * @param idCliente ID del cliente
     * @return Lista de ventas del cliente
     */

    public List<Venta> buscarPorCliente(int idCliente) {
        // Verificar si el cliente existe
        Optional<Cliente> cliente = clienteDao.findById(idCliente);
        if (!cliente.isPresent()) {
            logger.warn("No se encontró el cliente con ID: " + idCliente);
            throw new IllegalArgumentException("No se encontró el cliente");
        }
        
        return ventaDao.findByCliente(idCliente);
    }
    public List<MetodoPagoResumen> obtenerMetodosPagoMasUtilizados(LocalDate inicio, LocalDate fin) {
        // delega a ReporteUtil
        return new ReporteUtil().obtenerMetodosPagoMasUtilizados(inicio, fin);
    }


    public int contarVentasEnRango(LocalDate desde, LocalDate hasta) {
        try { return ventaDao.contarVentasEnRango(desde, hasta);
        } catch (SQLException e) { logger.error("", e); return 0; }
    }
    /**
     * Busca ventas por fecha.
     * @param fecha Fecha de las ventas
     * @return Lista de ventas de la fecha especificada
     */
    public List<Venta> buscarPorFecha(LocalDate fecha) {
        return ventaDao.findByFecha(fecha);
    }
    
    /**
     * Busca ventas por rango de fechas.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de ventas en el rango de fechas especificado
     */
    public List<Venta> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        // Validar que la fecha de inicio no sea posterior a la fecha de fin
        if (fechaInicio.isAfter(fechaFin)) {
            logger.warn("La fecha de inicio no puede ser posterior a la fecha de fin");
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        
        return ventaDao.findByRangoFechas(fechaInicio, fechaFin);
    }
    /**
     * Obtiene las ventas agrupadas por ciudad de todos los clientes,
     * incluyendo la lista de productos vendidos.
     */
    public List<ReporteController.VentaPorCiudad> obtenerVentasPorCiudad() {
        List<ReporteController.VentaPorCiudad> resultado = new ArrayList<>();
        String sql =
                "SELECT c.ciudad, " +
                        "       COUNT(DISTINCT v.id_venta)      AS cantidad_ventas, " +
                        "       SUM(v.total)                    AS total_vendido, " +
                        "       STRING_AGG(DISTINCT p.nombre, ', ' ORDER BY p.nombre) AS productos " +
                        "FROM ventas v " +
                        " JOIN clientes c ON v.id_cliente    = c.id_cliente " +
                        " JOIN detalle_venta d ON v.id_venta  = d.id_venta " +
                        " JOIN productos p ON d.id_producto   = p.id_producto " +
                        "GROUP BY c.ciudad " +
                        "ORDER BY c.ciudad";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                resultado.add(new ReporteController.VentaPorCiudad(
                        rs.getString("ciudad"),
                        rs.getInt("cantidad_ventas"),
                        rs.getDouble("total_vendido"),
                        rs.getString("productos")    // <-- aquí capturas la lista
                ));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener ventas por ciudad", e);
        }

        return resultado;
    }


    /**
     * Obtiene las ventas agrupadas por ciudad de un cliente específico,
     * incluyendo la lista de productos vendidos.
     */
    public List<ReporteController.VentaPorCiudad> obtenerVentasPorCiudad(String ciudad) {
        List<ReporteController.VentaPorCiudad> resultado = new ArrayList<>();
        String sql =
                "SELECT c.ciudad, " +
                        "       COUNT(DISTINCT v.id_venta)                        AS cantidad_ventas, " +
                        "       SUM(v.total)                                      AS total_vendido, " +
                        "       STRING_AGG(DISTINCT p.nombre, ', ' ORDER BY p.nombre) AS productos " +
                        "FROM ventas v " +
                        " JOIN clientes c ON v.id_cliente   = c.id_cliente " +
                        " JOIN detalle_venta d ON v.id_venta = d.id_venta " +
                        " JOIN productos p ON d.id_producto  = p.id_producto " +
                        "WHERE c.ciudad = ? " +
                        "GROUP BY c.ciudad";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ciudad);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    resultado.add(new ReporteController.VentaPorCiudad(
                            rs.getString("ciudad"),
                            rs.getInt("cantidad_ventas"),
                            rs.getDouble("total_vendido"),
                            rs.getString("productos")      // <-- aquí el cuarto parámetro
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener ventas por ciudad específica", e);
        }

        return resultado;
    }

    /**
     * Busca ventas por método de pago.
     * @param metodoPago Método de pago
     * @return Lista de ventas con el método de pago especificado
     */
    public List<Venta> buscarPorMetodoPago(String metodoPago) {
        return ventaDao.findByMetodoPago(metodoPago);
    }


    /**
     * Registra una venta, sus detalles y actualiza el stock.
     * @param carrito Lista de productos en carrito
     * @param cliente Cliente (ya existente o recién registrado)
     * @param usuario Usuario que realiza la venta
     * @param metodoPago Método de pago (Efectivo, Tarjeta, Transferencia)
     * @return true si la venta fue registrada correctamente
     */
    public boolean registrarVenta(List<ProductoEnCarrito> carrito, Cliente cliente, Usuario usuario, String metodoPago) {
        Connection conn = null;
        try {
            conn = com.mistrapitos.utils.DatabaseUtil.getConnection();
            conn.setAutoCommit(false);

            // Si el cliente no tiene ID, lo insertamos (esto es opcional, normalmente ya debe existir)
            if (cliente.getIdCliente() <= 0) {
                int idCliente = clienteDao.insertar(cliente, conn);
                cliente.setIdCliente(idCliente);
            }

            // Calcular total
            double total = carrito.stream().mapToDouble(ProductoEnCarrito::getSubtotal).sum();

            // Registrar venta
            Venta venta = new Venta();
            venta.setIdCliente(cliente.getIdCliente());
            venta.setMetodoPago(metodoPago);
            venta.setTotal(total);
            int idVenta = ventaDao.insertar(venta, conn);

            // Registrar detalle y actualizar stock
            for (ProductoEnCarrito item : carrito) {
                ventaDao.insertarDetalleVenta(idVenta, item.getIdProducto(), item.getCantidad(), item.getPrecioUnitario(), item.getDescuento(), conn);
                productoDao.actualizarStock(item.getIdProducto(), -item.getCantidad(), conn);
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            logger.error("Error al registrar venta", e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
    
    /**
     * Actualiza una venta existente.
     * @param venta Venta a actualizar
     * @return Venta actualizada
     */
    public Venta actualizarVenta(Venta venta) {
        // Verificar si la venta existe
        Optional<Venta> existente = ventaDao.findById(venta.getIdVenta());
        if (!existente.isPresent()) {
            logger.warn("No se encontró la venta con ID: " + venta.getIdVenta());
            throw new IllegalArgumentException("No se encontró la venta");
        }
        
        // Validar datos de la venta
        if (venta.getDetalles().isEmpty()) {
            logger.warn("La venta debe tener al menos un detalle");
            throw new IllegalArgumentException("La venta debe tener al menos un detalle");
        }
        
        if (venta.getMetodoPago() == null || venta.getMetodoPago().trim().isEmpty()) {
            logger.warn("El método de pago es obligatorio");
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        
        // Verificar si el cliente existe (si se especificó)
        if (venta.getIdCliente() > 0) {
            Optional<Cliente> cliente = clienteDao.findById(venta.getIdCliente());
            if (!cliente.isPresent()) {
                logger.warn("No se encontró el cliente con ID: " + venta.getIdCliente());
                throw new IllegalArgumentException("No se encontró el cliente");
            }
            venta.setClienteNombre(cliente.get().getNombre());
        }
        
        // Verificar stock y obtener datos de productos
        for (DetalleVenta detalle : venta.getDetalles()) {
            Optional<Producto> producto = productoDao.findById(detalle.getIdProducto());
            if (!producto.isPresent()) {
                logger.warn("No se encontró el producto con ID: " + detalle.getIdProducto());
                throw new IllegalArgumentException("No se encontró el producto con ID: " + detalle.getIdProducto());
            }
            
            // Para actualización, no verificamos stock ya que se restaurará el stock de los detalles anteriores
            
            detalle.setProductoNombre(producto.get().getNombre());
            detalle.setPrecioUnitario(producto.get().getPrecio());
            detalle.setDescuentoAplicado(producto.get().getDescuento());
        }
        
        // Calcular total si no se especificó
        if (venta.getTotal() <= 0) {
            double total = 0;
            for (DetalleVenta detalle : venta.getDetalles()) {
                total += detalle.getSubtotal();
            }
            venta.setTotal(total);
        }
        
        return ventaDao.update(venta);
    }
    
    /**
     * Elimina una venta.
     * @param id ID de la venta a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    public boolean eliminarVenta(int id) {
        // Verificar si la venta existe
        Optional<Venta> existente = ventaDao.findById(id);
        if (!existente.isPresent()) {
            logger.warn("No se encontró la venta con ID: " + id);
            throw new IllegalArgumentException("No se encontró la venta");
        }
        
        return ventaDao.delete(id);
    }
    
    /**
     * Obtiene el total de ventas por día.
     * @param fecha Fecha para la cual se quiere obtener el total
     * @return Total de ventas del día
     */
    public double obtenerTotalVentasPorDia(LocalDate fecha) {
        return ventaDao.getTotalVentasPorDia(fecha);
    }
    
    /**
     * Obtiene el total de ventas por mes.
     * @param año Año
     * @param mes Mes (1-12)
     * @return Total de ventas del mes
     */
    public double obtenerTotalVentasPorMes(int año, int mes) {
        // Validar mes
        if (mes < 1 || mes > 12) {
            logger.warn("El mes debe estar entre 1 y 12");
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }
        
        return ventaDao.getTotalVentasPorMes(año, mes);
    }
    
    /**
     * Obtiene el número de ventas por día.
     * @param fecha Fecha para la cual se quiere obtener el número de ventas
     * @return Número de ventas del día
     */
    public int obtenerNumeroVentasPorDia(LocalDate fecha) {
        return ventaDao.getNumeroVentasPorDia(fecha);
    }
}