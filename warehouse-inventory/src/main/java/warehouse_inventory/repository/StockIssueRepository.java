package warehouse_inventory.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import warehouse_inventory.entity.StockIssue;

public interface StockIssueRepository
        extends JpaRepository<StockIssue, Long> {

    boolean existsByIssueNumberIgnoreCase(String issueNumber);

    boolean existsByIssueNumberIgnoreCaseAndIdNot(
            String issueNumber, Long id);

    @Query("""
        SELECT COALESCE(SUM(i.quantity), 0L)
        FROM StockIssue i
        WHERE i.item.id = :itemId
        AND i.warehouse.id = :warehouseId
        """)
    Long totalIssued(
            @Param("itemId") Long itemId,
            @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM StockIssue i WHERE i.id = :id")
    Optional<StockIssue> findByIdForUpdate(
            @Param("id") Long id);
}