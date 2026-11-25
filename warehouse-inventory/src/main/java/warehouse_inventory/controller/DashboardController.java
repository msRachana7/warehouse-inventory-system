package warehouse_inventory.controller;

import org.springframework.web.bind.annotation.*;

import warehouse_inventory.dto.DashboardResponse;
import warehouse_inventory.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public DashboardResponse getSummary() {
        return service.getSummary();
    }
}