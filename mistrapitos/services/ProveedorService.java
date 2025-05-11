package com.mistrapitos.services;

import com.mistrapitos.dao.ProveedorDao;
import com.mistrapitos.models.Proveedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import com.mistrapitos.models.Producto;
import com.mistrapitos.dao.ProductoDao;
import com.mistrapitos.dao.VentaDao;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de proveedores.
 */
public class ProveedorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProveedorService.class);
    private final ProveedorDao proveedorDao;
    private final ProductoDao productoDao   = new ProductoDao();
    
    /**
     * Constructor.
     */
    public ProveedorService() {
        this.proveedorDao = new ProveedorDao();
    }
    
    /**
     * Obtiene todos los proveedores.
     * @return Lista de proveedores
     */
    public List<Proveedor> obtenerTodos() {
        return proveedorDao.findAll();
    }
    
    /**
     * Obtiene un proveedor por su ID.
     * @param id ID del proveedor
     * @return Proveedor encontrado o vacío si no existe
     */
    public Optional<Proveedor> obtenerPorId(int id) {
        return proveedorDao.findById(id);
    }
    
    /**
     * Busca proveedores por nombre, contacto o correo.
     * @param termino Término de búsqueda
     * @return Lista de proveedores que coinciden con el término
     */
    public List<Proveedor> buscarPorTermino(String termino) {
        return proveedorDao.findByTermino(termino);
    }
    
    /**
     * Guarda un nuevo proveedor.
     * @param proveedor Proveedor a guardar
     * @return Proveedor guardado con su ID generado
     */
    public Proveedor guardar(Proveedor proveedor) {
        // Validar datos del proveedor
        if (proveedor.getNombre() == null || proveedor.getNombre().trim().isEmpty()) {
            logger.warn("El nombre del proveedor es obligatorio");
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        
        return proveedorDao.save(proveedor);
    }
    /**
     * Obtiene los productos de un proveedor que estén en stock.
     */
    public List<Producto> obtenerProductosDeProveedorEnInventario(int idProveedor) {
        Optional<Proveedor> opt = proveedorDao.findById(idProveedor);
        if (opt.isEmpty()) return Collections.emptyList();
        String csv = opt.get().getProductosVendidos();
        List<Producto> salida = new ArrayList<>();
        for (String idStr : csv.split(",")) {
            int id = Integer.parseInt(idStr.trim());
            productoDao.findById(id)
                    .filter(p -> p.getStock() > 0)
                    .ifPresent(salida::add);
        }
        return salida;
    }

    /**
     * Actualiza un proveedor existente.
     * @param proveedor Proveedor a actualizar
     * @return Proveedor actualizado
     */
    public Proveedor actualizar(Proveedor proveedor) {
        // Verificar si el proveedor existe
        Optional<Proveedor> existente = proveedorDao.findById(proveedor.getIdProveedor());
        if (!existente.isPresent()) {
            logger.warn("No se encontró el proveedor con ID: " + proveedor.getIdProveedor());
            throw new IllegalArgumentException("No se encontró el proveedor");
        }
        
        // Validar datos del proveedor
        if (proveedor.getNombre() == null || proveedor.getNombre().trim().isEmpty()) {
            logger.warn("El nombre del proveedor es obligatorio");
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        
        return proveedorDao.update(proveedor);
    }
    
    /**
     * Elimina un proveedor.
     * @param id ID del proveedor a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    public boolean eliminar(int id) {
        // Verificar si el proveedor existe
        Optional<Proveedor> existente = proveedorDao.findById(id);
        if (!existente.isPresent()) {
            logger.warn("No se encontró el proveedor con ID: " + id);
            throw new IllegalArgumentException("No se encontró el proveedor");
        }
        
        // Verificar si el proveedor tiene productos asociados
        if (proveedorDao.tieneProductosAsociados(id)) {
            logger.warn("El proveedor tiene productos asociados y no puede ser eliminado: " + id);
            throw new IllegalArgumentException("El proveedor tiene productos asociados y no puede ser eliminado");
        }
        
        return proveedorDao.delete(id);
    }
    
    /**
     * Obtiene los productos asociados a un proveedor.
     * @param idProveedor ID del proveedor
     * @return Lista de IDs de productos asociados al proveedor
     */
    public List<Integer> obtenerProductosAsociados(int idProveedor) {
        // Verificar si el proveedor existe
        Optional<Proveedor> existente = proveedorDao.findById(idProveedor);
        if (!existente.isPresent()) {
            logger.warn("No se encontró el proveedor con ID: " + idProveedor);
            throw new IllegalArgumentException("No se encontró el proveedor");
        }
        
        return proveedorDao.getProductosAsociados(idProveedor);
    }
}