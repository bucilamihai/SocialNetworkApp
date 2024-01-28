package ir.map.repository.paging;

import ir.map.domain.Entity;
import ir.map.repository.Repository;

public interface PagingRepository<ID,
        E extends Entity<ID>>
        extends Repository<ID, E> {

    Page<E> findAll(Pageable pageable);
}
