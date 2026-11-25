package warehouse_inventory.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import warehouse_inventory.entity.StockReceipt;

public interface StockReceiptRepository
        extends JpaRepository<StockReceipt, Long> {

    boolean existsByReceiptNumberIgnoreCase(String receiptNumber);

    boolean existsByReceiptNumberIgnoreCaseAndIdNot(
            String receiptNumber, Long id);

    @Query("""
        SELECT COALESCE(SUM(r.quantity), 0L)
        FROM StockReceipt r
        WHERE r.item.id = :itemId
        AND r.warehouse.id = :warehouseId
        """)
    Long totalReceived(
            @Param("itemId") Long itemId,
            @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReceipt r WHERE r.id = :id")
    Optional<StockReceipt> findByIdForUpdate(
            @Param("id") Long id);
}