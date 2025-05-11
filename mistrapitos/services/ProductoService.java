package com.mistrapitos.services;

import com.mistrapitos.dao.CategoriaDao;
import com.mistrapitos.dao.ProductoDao;
import com.mistrapitos.models.Categoria;
import com.mistrapitos.models.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de productos.
 */
public class ProductoService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductoService.class);
    private final ProductoDao productoDao;
    private final CategoriaDao categoriaDao;
    
    /**
     * Constructor.
     */
    public ProductoService() {
        this.productoDao = new ProductoDao();
        this.categoriaDao = new CategoriaDao();
    }
    
    /**
     * Obtiene todos los productos.
     * @return Lista de productos
     */
    public List<Producto> obtenerTodos() {
        return productoDao.findAll();
    }
    
    /**
     * Obtiene un producto por su ID.
     * @param id ID del producto
     * @return Producto encontrado o vacío si no existe
     */
    public Optional<Producto> obtenerPorId(int id) {
        return productoDao.findById(id);
    }
    
    /**
     * Busca productos por nombre o código de barras.
     * @param termino Término de búsqueda
     * @return Lista de productos que coinciden con el término
     */
    public List<Producto> buscarPorNombreOBarcode(String termino) {
        return productoDao.findByNombreOrBarcode(termino);
    }
    
    /**
     * Busca productos por categoría.
     * @param idCategoria ID de la categoría
     * @return Lista de productos de la categoría especificada
     */
    public List<Producto> buscarPorCategoria(int idCategoria) {
        return productoDao.findByCategoria(idCategoria);
    }
    
    /**
     * Busca productos con stock bajo.
     * @param stockMinimo Stock mínimo
     * @return Lista de productos con stock bajo
     */
    public List<Producto> buscarPorStockBajo(int stockMinimo) {
        return productoDao.findByStockBajo(stockMinimo);
    }
    
    /**
     * Guarda un nuevo producto.
     * @param producto Producto a guardar
     * @return Producto guardado con su ID generado
     */
    public Producto guardar(Producto producto) {
        // Verificar si la categoría existe
        if (producto.getIdCategoria() > 0) {
            Optional<Categoria> categoria = categoriaDao.findById(producto.getIdCategoria());
            if (!categoria.isPresent()) {
                logger.warn("No se encontró la categoría con ID: " + producto.getIdCategoria());
                throw new IllegalArgumentException("No se encontró la categoría");
            }
            producto.setCategoriaNombre(categoria.get().getNombre());
        }
        
        return productoDao.save(producto);
    }
    /**
     * Busca un producto por su código de barras.
     * @param barcode Código de barras
     * @return Producto encontrado o vacío si no existe
     */
    public Optional<Producto> buscarPorBarcode(String barcode) {
        return productoDao.buscarPorBarcode(barcode);
    }

    /**
     * Guarda una nueva categoría utilizando solo el nombre.
     * @param nombre Nombre de la categoría a guardar
     * @return Categoría guardada con su ID generado
     */
    public Categoria guardarCategoria(String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return guardarCategoria(categoria);
    }

    public Optional<Producto> obtenerProductoConMasStock() {
        return productoDao.findProductoConMasStock();
    }

    public List<Producto> filtrarPorPrecioYStock(double precioMinimo) {
        return productoDao.findByPrecioYStock(precioMinimo);
    }
    /**
     * Actualiza un producto existente.
     * @param producto Producto a actualizar
     * @return Producto actualizado
     */
    public Producto actualizar(Producto producto) {
        // Verificar si el producto existe
        Optional<Producto> existente = productoDao.findById(producto.getIdProducto());
        if (!existente.isPresent()) {
            logger.warn("No se encontró el producto con ID: " + producto.getIdProducto());
            throw new IllegalArgumentException("No se encontró el producto");
        }
        
        // Verificar si la categoría existe
        if (producto.getIdCategoria() > 0) {
            Optional<Categoria> categoria = categoriaDao.findById(producto.getIdCategoria());
            if (!categoria.isPresent()) {
                logger.warn("No se encontró la categoría con ID: " + producto.getIdCategoria());
                throw new IllegalArgumentException("No se encontró la categoría");
            }
            producto.setCategoriaNombre(categoria.get().getNombre());
        }
        
        return productoDao.update(producto);
    }
    
    /**
     * Actualiza el stock de un producto.
     * @param idProducto ID del producto
     * @param cantidad Cantidad a agregar o restar del stock
     * @return true si se actualizó correctamente, false en caso contrario
     */
    public boolean actualizarStock(int idProducto, int cantidad) {
        // Verificar si el producto existe
        Optional<Producto> existente = productoDao.findById(idProducto);
        if (!existente.isPresent()) {
            logger.warn("No se encontró el producto con ID: " + idProducto);
            throw new IllegalArgumentException("No se encontró el producto");
        }
        
        // Verificar que el stock no quede negativo
        if (existente.get().getStock() + cantidad < 0) {
            logger.warn("El stock del producto no puede ser negativo: " + idProducto);
            throw new IllegalArgumentException("El stock del producto no puede ser negativo");
        }
        
        return productoDao.updateStock(idProducto, cantidad);
    }
    
    /**
     * Elimina un producto.
     * @param id ID del producto a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    public boolean eliminar(int id) {
        // Verificar si el producto existe
        Optional<Producto> existente = productoDao.findById(id);
        if (!existente.isPresent()) {
            logger.warn("No se encontró el producto con ID: " + id);
            throw new IllegalArgumentException("No se encontró el producto");
        }
        
        return productoDao.delete(id);
    }
    
    /**
     * Obtiene todas las categorías.
     * @return Lista de categorías
     */
    public List<Categoria> obtenerTodasCategorias() {
        return categoriaDao.findAll();
    }
    
    /**
     * Guarda una nueva categoría.
     * @param categoria Categoría a guardar
     * @return Categoría guardada con su ID generado
     */
    public Categoria guardarCategoria(Categoria categoria) {
        // Verificar si el nombre de la categoría ya existe
        Optional<Categoria> existente = categoriaDao.findByNombre(categoria.getNombre());
        if (existente.isPresent()) {
            logger.warn("El nombre de la categoría ya existe: " + categoria.getNombre());
            throw new IllegalArgumentException("El nombre de la categoría ya existe");
        }
        
        return categoriaDao.save(categoria);
    }
    
    /**
     * Actualiza una categoría existente.
     * @param categoria Categoría a actualizar
     * @return Categoría actualizada
     */
    public Categoria actualizarCategoria(Categoria categoria) {
        // Verificar si la categoría existe
        Optional<Categoria> existente = categoriaDao.findById(categoria.getIdCategoria());
        if (!existente.isPresent()) {
            logger.warn("No se encontró la categoría con ID: " + categoria.getIdCategoria());
            throw new IllegalArgumentException("No se encontró la categoría");
        }
        
        // Verificar si el nombre de la categoría ya existe (si se está cambiando)
        if (!existente.get().getNombre().equals(categoria.getNombre())) {
            Optional<Categoria> existenteNombre = categoriaDao.findByNombre(categoria.getNombre());
            if (existenteNombre.isPresent()) {
                logger.warn("El nombre de la categoría ya existe: " + categoria.getNombre());
                throw new IllegalArgumentException("El nombre de la categoría ya existe");
            }
        }
        
        return categoriaDao.update(categoria);
    }
    
    /**
     * Elimina una categoría.
     * @param id ID de la categoría a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    public boolean eliminarCategoria(int id) {
        // Verificar si la categoría existe
        Optional<Categoria> existente = categoriaDao.findById(id);
        if (!existente.isPresent()) {
            logger.warn("No se encontró la categoría con ID: " + id);
            throw new IllegalArgumentException("No se encontró la categoría");
        }
        
        // Verificar si la categoría está siendo utilizada
        if (categoriaDao.isInUse(id)) {
            logger.warn("La categoría está siendo utilizada por productos: " + id);
            throw new IllegalArgumentException("La categoría está siendo utilizada por productos");
        }
        
        return categoriaDao.delete(id);
    }
}