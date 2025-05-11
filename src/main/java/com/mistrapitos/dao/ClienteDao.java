
package com.mistrapitos.dao;

import com.mistrapitos.models.Cliente;
import com.mistrapitos.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del DAO para la entidad Cliente.
 */
public class ClienteDao implements Dao<Cliente, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ClienteDao.class);

    /**
     * Obtiene todos los clientes de la base de datos.
     * @return Lista de clientes
     */
    @Override
    public List<Cliente> findAll() {
        String sql = "SELECT id_cliente,nombre,direccion,correo,telefono,ciudad FROM clientes";
        List<Cliente> lista = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Cliente c = new Cliente(
                        rs.getInt("id_cliente"),
                        rs.getString("nombre"),
                        rs.getString("direccion"),
                        rs.getString("correo"),
                        rs.getString("telefono"),
                        rs.getString("ciudad")
                );
                lista.add(c);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los clientes", e);
        }
        return lista;
    }

    /**
     * Obtiene un cliente por su ID.
     * @param id ID del cliente
     * @return Cliente encontrado o vacío si no existe
     */
    @Override
    public Optional<Cliente> findById(Integer id) {
        String sql = "SELECT id_cliente, nombre, direccion, correo, telefono, ciudad FROM clientes WHERE id_cliente = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Cliente cliente = new Cliente(
                            rs.getInt("id_cliente"),
                            rs.getString("nombre"),
                            rs.getString("direccion"),
                            rs.getString("correo"),
                            rs.getString("telefono"),
                            rs.getString("ciudad")
                    );
                    return Optional.of(cliente);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener cliente por ID: " + id, e);
        }

        return Optional.empty();
    }

    /**
     * Busca clientes por nombre, correo o teléfono.
     * @param termino Término de búsqueda
     * @return Lista de clientes que coinciden con el término
     */
    public List<Cliente> findByTermino(String termino) {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT id_cliente, nombre, direccion, correo, telefono, ciudad FROM clientes " +
                "WHERE nombre ILIKE ? OR correo ILIKE ? OR telefono ILIKE ? " +
                "ORDER BY nombre";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchTerm = "%" + termino + "%";
            stmt.setString(1, searchTerm);
            stmt.setString(2, searchTerm);
            stmt.setString(3, searchTerm);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cliente cliente = new Cliente(
                            rs.getInt("id_cliente"),
                            rs.getString("nombre"),
                            rs.getString("direccion"),
                            rs.getString("correo"),
                            rs.getString("telefono"),
                            rs.getString("ciudad")
                    );
                    clientes.add(cliente);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar clientes por término: " + termino, e);
        }

        return clientes;
    }

    /**
     * Guarda un nuevo cliente en la base de datos.
     * @param cliente Cliente a guardar
     * @return Cliente guardado con su ID generado
     */
    @Override
    public Cliente save(Cliente cliente) {
        String sql = "INSERT INTO clientes (nombre, direccion, correo, telefono, ciudad) VALUES (?, ?, ?, ?, ?) RETURNING id_cliente";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getDireccion());
            stmt.setString(3, cliente.getCorreo());
            stmt.setString(4, cliente.getTelefono());
            stmt.setString(5, cliente.getCiudad());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    cliente.setIdCliente(rs.getInt(1));
                    logger.info("Cliente guardado correctamente: " + cliente.getNombre());
                    return cliente;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al guardar cliente: " + cliente.getNombre(), e);
        }

        return cliente;
    }

    /**
     * Inserta un cliente usando una conexión existente (para transacciones).
     * @param cliente Cliente a insertar
     * @param conn Conexión activa
     * @return ID generado del cliente, o -1 si falla
     */
    public int insertar(Cliente cliente, Connection conn) {
        String sql = "INSERT INTO clientes (nombre, direccion, correo, telefono, ciudad) VALUES (?, ?, ?, ?, ?) RETURNING id_cliente";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getDireccion());
            stmt.setString(3, cliente.getCorreo());
            stmt.setString(4, cliente.getTelefono());
            stmt.setString(5, cliente.getCiudad());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id_cliente");
                    cliente.setIdCliente(id);
                    logger.info("Cliente guardado correctamente (transacción): " + cliente.getNombre());
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al guardar cliente (transacción): " + cliente.getNombre(), e);
        }
        return -1;
    }

    /**
     * Busca un cliente por nombre (exacto) o por ID si el string es numérico, usando una conexión existente.
     * @param nombreOId Nombre o ID del cliente
     * @param conn Conexión activa
     * @return Optional con el cliente si existe
     */
    public Optional<Cliente> buscarPorNombreOId(String nombreOId, Connection conn) {
        try {
            // Si es numérico, buscar por ID
            if (nombreOId != null && nombreOId.matches("\\d+")) {
                int id = Integer.parseInt(nombreOId);
                String sql = "SELECT * FROM clientes WHERE id_cliente = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Cliente cliente = new Cliente(
                                    rs.getInt("id_cliente"),
                                    rs.getString("nombre"),
                                    rs.getString("direccion"),
                                    rs.getString("correo"),
                                    rs.getString("telefono"),
                                    rs.getString("ciudad")
                            );
                            return Optional.of(cliente);
                        }
                    }
                }
            }
            // Buscar por nombre exacto
            String sql = "SELECT * FROM clientes WHERE nombre = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, nombreOId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Cliente cliente = new Cliente(
                                rs.getInt("id_cliente"),
                                rs.getString("nombre"),
                                rs.getString("direccion"),
                                rs.getString("correo"),
                                rs.getString("telefono"),
                                rs.getString("ciudad")
                        );
                        return Optional.of(cliente);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar cliente por nombre o ID: " + nombreOId, e);
        }
        return Optional.empty();
    }

    /**
     * Actualiza un cliente existente en la base de datos.
     * @param cliente Cliente a actualizar
     * @return Cliente actualizado
     */
    @Override
    public Cliente update(Cliente cliente) {
        String sql = "UPDATE clientes SET nombre = ?, direccion = ?, correo = ?, telefono = ?, ciudad = ? WHERE id_cliente = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getDireccion());
            stmt.setString(3, cliente.getCorreo());
            stmt.setString(4, cliente.getTelefono());
            stmt.setString(5, cliente.getCiudad());
            stmt.setInt(6, cliente.getIdCliente());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Cliente actualizado correctamente: " + cliente.getNombre());
            } else {
                logger.warn("No se actualizó ningún cliente con ID: " + cliente.getIdCliente());
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar cliente: " + cliente.getNombre(), e);
        }

        return cliente;
    }

    /**
     * Elimina un cliente de la base de datos por su ID.
     * @param id ID del cliente a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM clientes WHERE id_cliente = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Cliente eliminado correctamente con ID: " + id);
                return true;
            } else {
                logger.warn("No se eliminó ningún cliente con ID: " + id);
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar cliente con ID: " + id, e);
        }

        return false;
    }

    /**
     * Verifica si un cliente tiene ventas asociadas.
     * @param id ID del cliente
     * @return true si el cliente tiene ventas, false en caso contrario
     */
    public boolean tieneVentas(Integer id) {
        String sql = "SELECT COUNT(*) FROM ventas WHERE id_cliente = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al verificar si el cliente tiene ventas: " + id, e);
        }

        return false;
    }

    /**
     * Obtiene el historial de compras de un cliente.
     * @param idCliente ID del cliente
     * @return Lista de IDs de ventas del cliente
     */
    public List<Integer> getHistorialCompras(Integer idCliente) {
        List<Integer> idsVentas = new ArrayList<>();
        String sql = "SELECT id_venta FROM ventas WHERE id_cliente = ? ORDER BY fecha DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    idsVentas.add(rs.getInt("id_venta"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener historial de compras del cliente: " + idCliente, e);
        }

        return idsVentas;
    }
}
