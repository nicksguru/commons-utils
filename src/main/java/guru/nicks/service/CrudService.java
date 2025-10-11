package guru.nicks.service;

import guru.nicks.exception.http.NotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Typical CRUD methods.
 *
 * @param <T>  entity type
 * @param <ID> primary key type
 */
@SuppressWarnings("java:S119")  // allow type names like 'ID'
public interface CrudService<T, ID> {

    /**
     * Saves (i.e. inserts/updates) entity in DB.
     *
     * @param entity entity to save
     * @return entity saved (should be used instead of the argument!)
     */
    T save(T entity);

    /**
     * Saves (i.e. inserts/updates) entities in DB.
     *
     * @param entities entities to save
     * @return entities saved (should be used instead of the argument!)
     */
    Iterable<T> saveAll(Iterable<T> entities);

    /**
     * Deletes entity from DB. Doesn't complain if there's no such entity in DB.
     *
     * @param entity entity to delete
     */
    void delete(T entity);

    /**
     * Deletes entity from DB. Doesn't complain if there's no such entity in DB.
     *
     * @param id entity ID
     */
    void deleteById(ID id);

    /**
     * Deletes entities from DB. Doesn't complain if there's no such entity in DB.
     *
     * @param ids entity IDs
     */
    void deleteAllById(Iterable<ID> ids);

    /**
     * Checks entity existence.
     *
     * @param id entity ID
     * @return true if entity exists
     */
    boolean existsById(ID id);

    /**
     * Finds entity by ID.
     *
     * @param id entity ID
     * @return optional entity
     */
    Optional<T> findById(ID id);

    /**
     * Retrieves entity by ID.
     *
     * @param id entity ID
     * @return entity
     * @throws NotFoundException no such entity
     */
    T getByIdOrThrow(ID id);

    /**
     * Returns elements in the same order as they're specified in arguments.
     *
     * @param ids IDs
     * @return elements in the same order as in {@code ids}
     */
    List<T> findAllByIdPreserveOrder(Collection<ID> ids);

    /**
     * Fetches all documents using DB cursor. Faster than {@link #findAll(Pageable)}.
     * <p>
     * WARNING: requires {@code try-with-resources}! Also, in case of JPA, consider calling
     * {@code EntityManager#detach(Object)} after processing each record to avoid OOM (Hibernate keeps references to the
     * entities fetched).
     *
     * @return stream of objects
     */
    Stream<T> findAllAsStream();

    /**
     * Fetches a page of results.
     *
     * @param pageable page and sorting definition
     * @return page
     */
    Page<T> findAll(Pageable pageable);

}
