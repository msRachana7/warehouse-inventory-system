package warehouse_inventory.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import warehouse_inventory.entity.Warehouse;
import warehouse_inventory.repository.WarehouseRepository;

@Service
public class WarehouseService {

    private final WarehouseRepository repository;

    public WarehouseService(WarehouseRepository repository) {
        this.repository = repository;
    }

    // Get all warehouses
    public List<Warehouse> getAllWarehouses() {
        return repository.findAll();
    }

    // Get warehouse by ID
    public Warehouse getWarehouseById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Warehouse not found"));
    }

    // Add warehouse
    @Transactional
    public Warehouse addWarehouse(Warehouse warehouse) {

        validateWarehouse(warehouse);

        if (repository.existsByWarehouseCodeIgnoreCase(
                warehouse.getWarehouseCode())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Warehouse code already exists");
        }

        Warehouse newWarehouse = new Warehouse();

        copyFields(warehouse, newWarehouse);
        newWarehouse.setActive(true);

        return repository.save(newWarehouse);
    }

    // Update warehouse
    @Transactional
    public Warehouse updateWarehouse(Long id, Warehouse warehouse) {

        Warehouse existing = getWarehouseById(id);

        validateWarehouse(warehouse);

        if (repository.existsByWarehouseCodeIgnoreCaseAndIdNot(
                warehouse.getWarehouseCode(), id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Warehouse code already exists");
        }

        copyFields(warehouse, existing);

        return repository.save(existing);
    }

    // Deactivate warehouse
    @Transactional
    public void deleteWarehouse(Long id) {

        Warehouse warehouse = getWarehouseById(id);

        warehouse.setActive(false);

        repository.save(warehouse);
    }

    // Validate input
    private void validateWarehouse(Warehouse warehouse) {

        if (warehouse.getWarehouseCode() == null ||
                warehouse.getWarehouseCode().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Warehouse code is required");
        }

        if (warehouse.getWarehouseName() == null ||
                warehouse.getWarehouseName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Warehouse name is required");
        }
    }

    // Copy fields
    private void copyFields(Warehouse source, Warehouse target) {

        target.setWarehouseCode(
                source.getWarehouseCode().trim());

        target.setWarehouseName(
                source.getWarehouseName().trim());

        target.setLocation(source.getLocation());
    }
}
