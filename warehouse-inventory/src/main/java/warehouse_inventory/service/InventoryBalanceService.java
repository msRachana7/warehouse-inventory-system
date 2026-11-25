package warehouse_inventory.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import warehouse_inventory.dto.InventoryBalanceResponse;
import warehouse_inventory.entity.Item;
import warehouse_inventory.entity.Warehouse;
import warehouse_inventory.repository.ItemRepository;
import warehouse_inventory.repository.WarehouseRepository;

@Service
public class InventoryBalanceService {

    private final InventoryService inventoryService;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryBalanceService(
            InventoryService inventoryService,
            ItemRepository itemRepository,
            WarehouseRepository warehouseRepository) {

        this.inventoryService = inventoryService;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryBalanceResponse> getBalances(
            Long warehouseId) {

        // Get active items
        List<Item> items = itemRepository.findAll()
                .stream()
                .filter(Item::isActive)
                .toList();

        List<Warehouse> warehouses;

        // Filter by warehouse if an ID is provided
        if (warehouseId != null) {

            Warehouse warehouse = warehouseRepository
                    .findById(warehouseId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Warehouse not found"));

            if (!warehouse.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Warehouse is inactive");
            }

            warehouses = List.of(warehouse);

        } else {

            warehouses = warehouseRepository.findAll()
                    .stream()
                    .filter(Warehouse::isActive)
                    .toList();
        }

        // Calculate stock for every item and warehouse
        return items.stream()
                .flatMap(item -> warehouses.stream()
                        .map(warehouse ->
                                createResponse(item, warehouse)))
                .toList();
    }

    private InventoryBalanceResponse createResponse(
            Item item, Warehouse warehouse) {

        long quantity = inventoryService.getBalance(
                item.getId(),
                warehouse.getId());

        boolean lowStock =
                quantity <= item.getReorderLevel();

        return new InventoryBalanceResponse(
                item.getId(),
                item.getItemCode(),
                item.getItemName(),
                warehouse.getId(),
                warehouse.getWarehouseName(),
                quantity,
                item.getReorderLevel(),
                lowStock
        );
    }
}