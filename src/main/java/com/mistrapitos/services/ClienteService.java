package com.mistrapitos.services;

import com.mistrapitos.dao.ClienteDao;
import com.mistrapitos.dao.VentaDao;
import com.mistrapitos.models.Cliente;
import com.mistrapitos.models.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mistrapitos.utils.DatabaseUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de clientes.
 */
public class ClienteService {

    private static final Logger logger = LoggerFactory.getLogger(ClienteService.class);
    private final ClienteDao clienteDao;
    private final VentaDao ventaDao;

    /**
     * Constructor.
     */
    public ClienteService() {
        this.clienteDao = new ClienteDao();
        this.ventaDao = new VentaDao();
    }

    /**
     * Busca un cliente por nombre exacto o por ID si el string es numérico.
     * @param nombreOId Nombre o ID del cliente
     * @return Optional con el cliente si existe
     */
    public Optional<Cliente> buscarPorNombreOId(String nombreOId) {
        if (nombreOId == null || nombreOId.trim().isEmpty()) return Optional.empty();
        try {
            // Si es numérico, buscar por ID
            if (nombreOId.matches("\\d+")) {
                int id = Integer.parseInt(nombreOId);
                return clienteDao.findById(id);
            }
            // Buscar por nombre exacto
            List<Cliente> lista = clienteDao.findByTermino(nombreOId);
            // Si hay coincidencia exacta, la retorna
            for (Cliente c : lista) {
                if (c.getNombre().equalsIgnoreCase(nombreOId)) {
                    return Optional.of(c);
                }
            }
            // Si no, retorna el primero que coincida parcialmente
            if (!lista.isEmpty()) return Optional.of(lista.get(0));
        } catch (Exception e) {
            logger.error("Error al buscar cliente por nombre o ID", e);
        }
        return Optional.empty();
    }

    /**
     * Registra un nuevo cliente y retorna el cliente guardado (con ID).
     * @param cliente Cliente a registrar
     * @return Cliente guardado o null si falla
     */
    public Cliente registrarCliente(Cliente cliente) {
        try {
            if (cliente.getNombre() == null || cliente.getNombre().trim().isEmpty()) {
                logger.warn("El nombre del cliente es obligatorio");
                throw new IllegalArgumentException("El nombre del cliente es obligatorio");
            }
            return clienteDao.save(cliente);
        } catch (Exception e) {
            logger.error("Error al registrar cliente", e);
            return null;
        }
    }

    // Métodos previos para compatibilidad

    public List<Cliente> obtenerTodos() {
        return clienteDao.findAll();
    }

    public Optional<Cliente> obtenerPorId(int id) {
        return clienteDao.findById(id);
    }

    public List<Cliente> buscarPorTermino(String termino) {
        return clienteDao.findByTermino(termino);
    }

    public Cliente guardar(Cliente cliente) {
        if (cliente.getNombre() == null || cliente.getNombre().trim().isEmpty()) {
            logger.warn("El nombre del cliente es obligatorio");
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        return clienteDao.save(cliente);
    }

    public Cliente actualizar(Cliente cliente) {
        Optional<Cliente> existente = clienteDao.findById(cliente.getIdCliente());
        if (!existente.isPresent()) {
            logger.warn("No se encontró el cliente con ID: " + cliente.getIdCliente());
            throw new IllegalArgumentException("No se encontró el cliente");
        }
        if (cliente.getNombre() == null || cliente.getNombre().trim().isEmpty()) {
            logger.warn("El nombre del cliente es obligatorio");
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        return clienteDao.update(cliente);
    }
    /**
     * Obtiene una lista de todas las ciudades únicas de los clientes
     */
    public List<String> obtenerCiudadesUnicas() {
        List<String> ciudades = new ArrayList<>();
        String sql = "SELECT DISTINCT ciudad FROM clientes WHERE ciudad IS NOT NULL ORDER BY ciudad";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ciudades.add(rs.getString("ciudad"));
            }
        } catch (Exception e) {
            logger.error("Error al obtener ciudades únicas", e);
        }

        return ciudades;
    }
    public boolean eliminar(int id) {
        Optional<Cliente> existente = clienteDao.findById(id);
        if (!existente.isPresent()) {
            logger.warn("No se encontró el cliente con ID: " + id);
            throw new IllegalArgumentException("No se encontró el cliente");
        }
        if (clienteDao.tieneVentas(id)) {
            logger.warn("El cliente tiene ventas asociadas y no puede ser eliminado: " + id);
            throw new IllegalArgumentException("El cliente tiene ventas asociadas y no puede ser eliminado");
        }
        return clienteDao.delete(id);
    }

    public List<Venta> obtenerHistorialCompras(int idCliente) {
        Optional<Cliente> existente = clienteDao.findById(idCliente);
        if (!existente.isPresent()) {
            logger.warn("No se encontró el cliente con ID: " + idCliente);
            throw new IllegalArgumentException("No se encontró el cliente");
        }
        List<Integer> idsVentas = clienteDao.getHistorialCompras(idCliente);
        List<Venta> ventas = new ArrayList<>();
        for (Integer idVenta : idsVentas) {
            Optional<Venta> venta = ventaDao.findById(idVenta);
            venta.ifPresent(ventas::add);
        }
        return ventas;
    }
}
