package com.mistrapitos.dao;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz genérica para los objetos de acceso a datos (DAO).
 * @param <T> Tipo de entidad
 * @param <ID> Tipo del identificador de la entidad
 */
public interface Dao<T, ID> {
    
    /**
     * Obtiene todas las entidades.
     * @return Lista de entidades
     */
    List<T> findAll();
    
    /**
     * Obtiene una entidad por su identificador.
     * @param id Identificador de la entidad
     * @return Entidad encontrada o vacío si no existe
     */
    Optional<T> findById(ID id);
    
    /**
     * Guarda una entidad.
     * @param entity Entidad a guardar
     * @return Entidad guardada
     */
    T save(T entity);
    
    /**
     * Actualiza una entidad.
     * @param entity Entidad a actualizar
     * @return Entidad actualizada
     */
    T update(T entity);
    
    /**
     * Elimina una entidad por su identificador.
     * @param id Identificador de la entidad
     * @return true si se eliminó correctamente, false en caso contrario
     */
    boolean delete(ID id);
}