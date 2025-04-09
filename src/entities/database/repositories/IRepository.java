package entities.database.repositories;

import java.util.List;
import java.util.Optional;

/**
 * A generic repository interface defining common data access operations.
 * @param <T> The type of the entity managed by the repository.
 * @param <ID> The type of the entity's identifier.
 */
public interface IRepository<T, ID> {
    /**
     * Saves a new entity or updates an existing one.
     * @param entity The entity to save or update.
     * @return The saved or updated entity.
     */
    T save(T entity);

    /**
     * Finds an entity by its ID.
     * @param id The ID of the entity to find.
     * @return An Optional containing the entity if found, otherwise empty.
     */
    Optional<T> findById(ID id);

    /**
     * Retrieves all entities managed by this repository.
     * @return A List of all entities.
     */
    List<T> findAll();

    /**
     * Deletes an entity by its ID.
     * @param id The ID of the entity to delete.
     * @return true if deletion was successful, false otherwise.
     */
    boolean deleteById(ID id);

     /**
     * Deletes a given entity.
     * @param entity The entity to delete.
     * @return true if deletion was successful, false otherwise.
     */
    boolean delete(T entity);

    /**
     * Counts the number of entities.
     * @return the total number of entities.
     */
    long count();
}