package warehouse_inventory.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import warehouse_inventory.dto.*;
import warehouse_inventory.service.StockTransferService;

@RestController
@RequestMapping("/api/stock-transfers")
@CrossOrigin(origins = "http://localhost:4200")
public class StockTransferController {

    private final StockTransferService service;

    public StockTransferController(StockTransferService service) {
        this.service = service;
    }

    // GET all transfers
    @GetMapping
    public List<StockTransferResponse> getAll() {
        return service.getAll();
    }

    // GET transfer by ID
    @GetMapping("/{id}")
    public StockTransferResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // POST create transfer
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockTransferResponse create(
            @Valid @RequestBody StockTransferRequest request) {

        return service.create(request);
    }

    // PUT update transfer
    @PutMapping("/{id}")
    public StockTransferResponse update(
            @PathVariable Long id,
            @Valid @RequestBody StockTransferRequest request) {

        return service.update(id, request);
    }

    // DELETE transfer
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}