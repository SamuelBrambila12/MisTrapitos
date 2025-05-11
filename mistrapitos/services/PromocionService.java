package com.mistrapitos.services;

import com.mistrapitos.dao.ProductoDao;
import com.mistrapitos.dao.PromocionDao;
import com.mistrapitos.models.Producto;
import com.mistrapitos.models.PromocionVista;
import com.mistrapitos.models.Promocion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de promociones.
 */
public class PromocionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PromocionService.class);
    private final PromocionDao promocionDao;
    private final ProductoDao productoDao;
    
    /**
     * Constructor.
     */
    public PromocionService() {
        this.promocionDao = new PromocionDao();
        this.productoDao = new ProductoDao();
    }
    
    /**
     * Obtiene todas las promociones.
     * @return Lista de promociones
     */
    public List<Promocion> obtenerTodas() {
        return promocionDao.findAll();
    }
    
    /**
     * Obtiene una promoción por su ID.
     * @param id ID de la promoción
     * @return Promoción encontrada o vacío si no existe
     */
    public Optional<Promocion> obtenerPorId(int id) {
        return promocionDao.findById(id);
    }
    /**
     * Obtiene la lista combinada de productos con descuentos directos y promociones.
     */
    public List<PromocionVista> obtenerPromocionesYDescuentos() {
        return promocionDao.obtenerPromocionesYDescuentos();
    }

    /**
     * Busca productos/promociones por nombre de producto.
     */
    public List<PromocionVista> buscarPromocionesYDescuentosPorNombre(String nombre) {
        return promocionDao.buscarPromocionesYDescuentosPorNombre(nombre);
    }

    /**
     * Guarda el descuento directo y/o la promoción temporal para un producto.
     * Si porcentajePromocion es null o 0, elimina la promoción temporal.
     * Si descuentoDirecto es distinto al actual, actualiza el producto.
     */
    public boolean guardarPromocionYDescuento(PromocionVista vista) {
        boolean ok = true;
        // Actualiza el descuento directo en productos
        ok &= productoDao.actualizarDescuentoDirecto(vista.getIdProducto(), vista.getDescuentoDirecto());
        // Maneja la promoción temporal
        if (vista.getPorcentajePromocion() != null && vista.getPorcentajePromocion() > 0) {
            ok &= promocionDao.guardarPromocionTemporal(
                    vista.getIdPromocion(),
                    vista.getIdProducto(),
                    vista.getPorcentajePromocion(),
                    vista.getFechaInicio(),
                    vista.getFechaFin()
            );
        } else {
            // Si no hay promoción, elimina la existente si la hay
            if (vista.getIdPromocion() != null) {
                ok &= promocionDao.eliminarPromocion(vista.getIdPromocion());
            }
        }
        return ok;
    }
    /**
     * Busca promociones por nombre de producto (parcial, insensible a mayúsculas).
     * @param nombreProducto Nombre (o parte) del producto
     * @return Lista de promociones que coinciden
     */
    public List<Promocion> buscarPorNombreProducto(String nombreProducto) {
        return promocionDao.findByNombreProducto(nombreProducto);
    }
    /**
     * Busca promociones por producto.
     * @param idProducto ID del producto
     * @return Lista de promociones del producto
     */
    public List<Promocion> buscarPorProducto(int idProducto) {
        // Verificar si el producto existe
        Optional<Producto> producto = productoDao.findById(idProducto);
        if (!producto.isPresent()) {
            logger.warn("No se encontró el producto con ID: " + idProducto);
            throw new IllegalArgumentException("No se encontró el producto");
        }
        
        return promocionDao.findByProducto(idProducto);
    }
    
    /**
     * Busca promociones activas en la fecha actual.
     * @return Lista de promociones activas
     */
    public List<Promocion> buscarActivas() {
        return promocionDao.findActivas();
    }
    
    /**
     * Busca promociones por rango de fechas.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de promociones en el rango de fechas
     */
    public List<Promocion> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        // Validar que la fecha de inicio no sea posterior a la fecha de fin
        if (fechaInicio.isAfter(fechaFin)) {
            logger.warn("La fecha de inicio no puede ser posterior a la fecha de fin");
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        
        return promocionDao.findByRangoFechas(fechaInicio, fechaFin);
    }
    
    /**
     * Guarda una nueva promoción.
     * @param promocion Promoción a guardar
     * @return Promoción guardada con su ID generado
     */
    public Promocion guardar(Promocion promocion) {
        // Validar datos de la promoción
        if (promocion.getPorcentajeDescuento() <= 0 || promocion.getPorcentajeDescuento() > 100) {
            logger.warn("El porcentaje de descuento debe estar entre 0 y 100");
            throw new IllegalArgumentException("El porcentaje de descuento debe estar entre 0 y 100");
        }
        
        if (promocion.getFechaInicio().isAfter(promocion.getFechaFin())) {
            logger.warn("La fecha de inicio no puede ser posterior a la fecha de fin");
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        
        // Verificar si el producto existe
        Optional<Producto> producto = productoDao.findById(promocion.getIdProducto());
        if (!producto.isPresent()) {
            logger.warn("No se encontró el producto con ID: " + promocion.getIdProducto());
            throw new IllegalArgumentException("No se encontró el producto");
        }
        
        promocion.setProductoNombre(producto.get().getNombre());
        
        return promocionDao.save(promocion);
    }
    
    /**
     * Actualiza una promoción existente.
     * @param promocion Promoción a actualizar
     * @return Promoción actualizada
     */
    public Promocion actualizar(Promocion promocion) {
        // Verificar si la promoción existe
        Optional<Promocion> existente = promocionDao.findById(promocion.getIdPromocion());
        if (!existente.isPresent()) {
            logger.warn("No se encontró la promoción con ID: " + promocion.getIdPromocion());
            throw new IllegalArgumentException("No se encontró la promoción");
        }
        
        // Validar datos de la promoción
        if (promocion.getPorcentajeDescuento() <= 0 || promocion.getPorcentajeDescuento() > 100) {
            logger.warn("El porcentaje de descuento debe estar entre 0 y 100");
            throw new IllegalArgumentException("El porcentaje de descuento debe estar entre 0 y 100");
        }
        
        if (promocion.getFechaInicio().isAfter(promocion.getFechaFin())) {
            logger.warn("La fecha de inicio no puede ser posterior a la fecha de fin");
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        
        // Verificar si el producto existe
        Optional<Producto> producto = productoDao.findById(promocion.getIdProducto());
        if (!producto.isPresent()) {
            logger.warn("No se encontró el producto con ID: " + promocion.getIdProducto());
            throw new IllegalArgumentException("No se encontró el producto");
        }
        
        promocion.setProductoNombre(producto.get().getNombre());
        
        return promocionDao.update(promocion);
    }
    
    /**
     * Elimina una promoción.
     * @param id ID de la promoción a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    public boolean eliminar(int id) {
        // Verificar si la promoción existe
        Optional<Promocion> existente = promocionDao.findById(id);
        if (!existente.isPresent()) {
            logger.warn("No se encontró la promoción con ID: " + id);
            throw new IllegalArgumentException("No se encontró la promoción");
        }
        
        return promocionDao.delete(id);
    }
}