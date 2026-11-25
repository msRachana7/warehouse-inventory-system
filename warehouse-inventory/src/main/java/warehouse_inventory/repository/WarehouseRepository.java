package warehouse_inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import warehouse_inventory.entity.Warehouse;

public interface WarehouseRepository
        extends JpaRepository<Warehouse, Long> {

    boolean existsByWarehouseCodeIgnoreCase(String warehouseCode);

    boolean existsByWarehouseCodeIgnoreCaseAndIdNot(
            String warehouseCode, Long id);
}