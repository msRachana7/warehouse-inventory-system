package warehouse_inventory.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import warehouse_inventory.dto.*;
import warehouse_inventory.service.StockReceiptService;

@RestController
@RequestMapping("/api/stock-receipts")
@CrossOrigin(origins = "http://localhost:4200")
public class StockReceiptController {

    private final StockReceiptService service;

    public StockReceiptController(StockReceiptService service) {
        this.service = service;
    }

    // GET all receipts
    @GetMapping
    public List<StockReceiptResponse> getAll() {
        return service.getAll();
    }

    // GET receipt by ID
    @GetMapping("/{id}")
    public StockReceiptResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // POST receipt
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockReceiptResponse create(
            @Valid @RequestBody StockReceiptRequest request) {

        return service.create(request);
    }

    // PUT receipt
    @PutMapping("/{id}")
    public StockReceiptResponse update(
            @PathVariable Long id,
            @Valid @RequestBody StockReceiptRequest request) {

        return service.update(id, request);
    }

    // DELETE receipt
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}