package warehouse_inventory.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import warehouse_inventory.entity.Item;

public interface ItemRepository
        extends JpaRepository<Item, Long> {

    boolean existsByItemCodeIgnoreCase(String itemCode);

    boolean existsByItemCodeIgnoreCaseAndIdNot(
            String itemCode, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);
}