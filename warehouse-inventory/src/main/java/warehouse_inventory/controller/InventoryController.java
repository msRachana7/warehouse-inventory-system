package warehouse_inventory.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import warehouse_inventory.dto.InventoryBalanceResponse;
import warehouse_inventory.service.InventoryBalanceService;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:4200")
public class InventoryController {

    private final InventoryBalanceService service;

    public InventoryController(
            InventoryBalanceService service) {

        this.service = service;
    }

    // GET all balances or filter by warehouse
    @GetMapping("/balances")
    public List<InventoryBalanceResponse> getBalances(
            @RequestParam(required = false)
            Long warehouseId) {

        return service.getBalances(warehouseId);
    }
}