package bioroute.dao;

import bioroute.exception.DatosException;
import java.util.ArrayList;

/**
 * Contrato generico para operaciones CRUD basicas de persistencia.
 * Aplica el principio de abstraccion y permite tratar de forma uniforme
 * a los DAO de las distintas entidades del dominio.
 *
 * @param <T> tipo de entidad gestionada por la implementacion concreta
 */
public interface CrudRepository<T> {

    /**
     * Inserta una entidad en la base de datos.
     * @return identificador generado por la base
     */
    int crear(T entidad) throws DatosException;

    /**
     * Recupera todas las entidades persistidas.
     */
    ArrayList<T> listar() throws DatosException;
}
