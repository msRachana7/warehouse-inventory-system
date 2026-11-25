package warehouse_inventory.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import warehouse_inventory.dto.*;
import warehouse_inventory.entity.*;
import warehouse_inventory.repository.*;

@Service
public class StockIssueService {

    private final StockIssueRepository issueRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    public StockIssueService(
            StockIssueRepository issueRepository,
            WarehouseRepository warehouseRepository,
            InventoryService inventoryService) {

        this.issueRepository = issueRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
    }

    // GET all issues
    @Transactional(readOnly = true)
    public List<StockIssueResponse> getAll() {

        return issueRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // GET issue by ID
    @Transactional(readOnly = true)
    public StockIssueResponse getById(Long id) {

        StockIssue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Issue not found"));

        return toResponse(issue);
    }

    // POST create issue
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StockIssueResponse create(StockIssueRequest request) {

        Item item = inventoryService.lockItem(request.itemId());
        Warehouse warehouse = getActiveWarehouse(
                request.warehouseId());

        checkActiveItem(item);

        String number = request.issueNumber().trim();

        if (issueRepository.existsByIssueNumberIgnoreCase(number)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Issue number already exists");
        }

        // Check available stock
        long available = inventoryService.getBalance(
                request.itemId(), request.warehouseId());

        inventoryService.checkBalance(
                available - request.quantity());

        StockIssue issue = new StockIssue();

        issue.setIssueNumber(number);
        issue.setItem(item);
        issue.setWarehouse(warehouse);
        issue.setQuantity(request.quantity());
        issue.setIssueDate(request.issueDate());

        return toResponse(issueRepository.save(issue));
    }

    // PUT update issue
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StockIssueResponse update(
            Long id, StockIssueRequest request) {

        StockIssue issue = issueRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Issue not found"));

        Long oldItemId = issue.getItem().getId();
        Long oldWarehouseId = issue.getWarehouse().getId();

        // Lock items to prevent simultaneous stock changes
        inventoryService.lockItems(
                oldItemId, request.itemId());

        Item newItem = inventoryService.lockItem(request.itemId());
        Warehouse newWarehouse = getActiveWarehouse(
                request.warehouseId());

        checkActiveItem(newItem);

        String number = request.issueNumber().trim();

        if (issueRepository.existsByIssueNumberIgnoreCaseAndIdNot(
                number, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Issue number already exists");
        }

        boolean sameLocation =
                oldItemId.equals(request.itemId())
                && oldWarehouseId.equals(request.warehouseId());

        if (sameLocation) {

            // Restore old issue, then apply new quantity
            long currentBalance = inventoryService.getBalance(
                    oldItemId, oldWarehouseId);

            long revisedBalance = currentBalance
                    + issue.getQuantity()
                    - request.quantity();

            inventoryService.checkBalance(revisedBalance);

        } else {

            // Old location receives its stock back.
            // New location must have enough stock.
            long destinationBalance = inventoryService.getBalance(
                    request.itemId(), request.warehouseId());

            inventoryService.checkBalance(
                    destinationBalance - request.quantity());
        }

        issue.setIssueNumber(number);
        issue.setItem(newItem);
        issue.setWarehouse(newWarehouse);
        issue.setQuantity(request.quantity());
        issue.setIssueDate(request.issueDate());

        return toResponse(issueRepository.save(issue));
    }

    // DELETE issue
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long id) {

        StockIssue issue = issueRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Issue not found"));

        // Lock the item while reversing its issue
        inventoryService.lockItem(issue.getItem().getId());

        // Removing an issue restores its quantity
        issueRepository.delete(issue);
    }

    // Check warehouse
    private Warehouse getActiveWarehouse(Long id) {

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Warehouse not found"));

        if (!warehouse.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Warehouse is inactive");
        }

        return warehouse;
    }

    // Check item
    private void checkActiveItem(Item item) {

        if (!item.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Item is inactive");
        }
    }

    // Convert entity to response
    private StockIssueResponse toResponse(StockIssue issue) {

        return new StockIssueResponse(
                issue.getId(),
                issue.getIssueNumber(),
                issue.getItem().getId(),
                issue.getWarehouse().getId(),
                issue.getQuantity(),
                issue.getIssueDate()
        );
    }
}