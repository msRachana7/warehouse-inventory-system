package warehouse_inventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import warehouse_inventory.entity.Warehouse;
import warehouse_inventory.service.WarehouseService;

@RestController
@RequestMapping("/api/warehouses")
@CrossOrigin(origins = "http://localhost:4200")
public class WarehouseController {

    private final WarehouseService service;

    public WarehouseController(WarehouseService service) {
        this.service = service;
    }

    // GET all warehouses
    @GetMapping
    public List<Warehouse> getAllWarehouses() {
        return service.getAllWarehouses();
    }

    // GET warehouse by ID
    @GetMapping("/{id}")
    public Warehouse getWarehouseById(@PathVariable Long id) {
        return service.getWarehouseById(id);
    }

    // POST add warehouse
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Warehouse addWarehouse(@RequestBody Warehouse warehouse) {
        return service.addWarehouse(warehouse);
    }

    // PUT update warehouse
    @PutMapping("/{id}")
    public Warehouse updateWarehouse(
            @PathVariable Long id,
            @RequestBody Warehouse warehouse) {

        return service.updateWarehouse(id, warehouse);
    }

    // DELETE deactivate warehouse
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWarehouse(@PathVariable Long id) {
        service.deleteWarehouse(id);
    }
}