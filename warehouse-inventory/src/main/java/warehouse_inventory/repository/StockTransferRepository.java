package warehouse_inventory.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import warehouse_inventory.entity.StockTransfer;
import warehouse_inventory.entity.TransferStatus;

public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {

    // Check duplicate transfer number
    boolean existsByTransferNumberIgnoreCase(String transferNumber);

    boolean existsByTransferNumberIgnoreCaseAndIdNot(
            String transferNumber, Long id);

    // Total stock transferred INTO a warehouse
    @Query("""
        SELECT COALESCE(SUM(t.quantity), 0L)
        FROM StockTransfer t
        WHERE t.item.id = :itemId
        AND t.destinationWarehouse.id = :warehouseId
        AND t.status = :status
        """)
    Long totalIncoming(
            @Param("itemId") Long itemId,
            @Param("warehouseId") Long warehouseId,
            @Param("status") TransferStatus status);

    // Total stock transferred OUT of a warehouse
    @Query("""
        SELECT COALESCE(SUM(t.quantity), 0L)
        FROM StockTransfer t
        WHERE t.item.id = :itemId
        AND t.sourceWarehouse.id = :warehouseId
        AND t.status = :status
        """)
    Long totalOutgoing(
            @Param("itemId") Long itemId,
            @Param("warehouseId") Long warehouseId,
            @Param("status") TransferStatus status);

    // Lock a transfer while editing or deleting
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM StockTransfer t WHERE t.id = :id")
    Optional<StockTransfer> findByIdForUpdate(
            @Param("id") Long id);
}