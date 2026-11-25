package warehouse_inventory.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import warehouse_inventory.dto.*;
import warehouse_inventory.service.StockIssueService;

@RestController
@RequestMapping("/api/stock-issues")
@CrossOrigin(origins = "http://localhost:4200")
public class StockIssueController {

    private final StockIssueService service;

    public StockIssueController(StockIssueService service) {
        this.service = service;
    }

    // GET all issues
    @GetMapping
    public List<StockIssueResponse> getAll() {
        return service.getAll();
    }

    // GET issue by ID
    @GetMapping("/{id}")
    public StockIssueResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // POST create issue
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockIssueResponse create(
            @Valid @RequestBody StockIssueRequest request) {

        return service.create(request);
    }

    // PUT update issue
    @PutMapping("/{id}")
    public StockIssueResponse update(
            @PathVariable Long id,
            @Valid @RequestBody StockIssueRequest request) {

        return service.update(id, request);
    }

    // DELETE issue
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}