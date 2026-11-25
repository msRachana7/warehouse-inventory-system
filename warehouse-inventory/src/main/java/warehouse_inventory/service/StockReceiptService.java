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
public class StockReceiptService {

    private final StockReceiptRepository receiptRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    public StockReceiptService(
            StockReceiptRepository receiptRepository,
            WarehouseRepository warehouseRepository,
            InventoryService inventoryService) {

        this.receiptRepository = receiptRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
    }

    // GET all receipts
    @Transactional(readOnly = true)
    public List<StockReceiptResponse> getAll() {

        return receiptRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // GET receipt by ID
    @Transactional(readOnly = true)
    public StockReceiptResponse getById(Long id) {

        StockReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Receipt not found"));

        return toResponse(receipt);
    }

    // POST create receipt
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StockReceiptResponse create(StockReceiptRequest request) {

        Item item = inventoryService.lockItem(request.itemId());
        Warehouse warehouse = getActiveWarehouse(request.warehouseId());

        checkActiveItem(item);

        String number = request.receiptNumber().trim();

        if (receiptRepository.existsByReceiptNumberIgnoreCase(number)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Receipt number already exists");
        }

        StockReceipt receipt = new StockReceipt();

        receipt.setReceiptNumber(number);
        receipt.setItem(item);
        receipt.setWarehouse(warehouse);
        receipt.setQuantity(request.quantity());
        receipt.setReceiptDate(request.receiptDate());

        return toResponse(receiptRepository.save(receipt));
    }

    // PUT update receipt
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StockReceiptResponse update(
            Long id, StockReceiptRequest request) {

        StockReceipt receipt = receiptRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Receipt not found"));

        Long oldItemId = receipt.getItem().getId();
        Long oldWarehouseId = receipt.getWarehouse().getId();

        // Lock affected items before checking stock
        inventoryService.lockItems(oldItemId, request.itemId());

        Item newItem = inventoryService.lockItem(request.itemId());
        Warehouse newWarehouse = getActiveWarehouse(
                request.warehouseId());

        checkActiveItem(newItem);

        String number = request.receiptNumber().trim();

        if (receiptRepository.existsByReceiptNumberIgnoreCaseAndIdNot(
                number, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Receipt number already exists");
        }

        // Check whether changing this receipt would
        // cause negative stock at the original warehouse
        long oldBalance = inventoryService.getBalance(
                oldItemId, oldWarehouseId);

        boolean sameLocation =
                oldItemId.equals(request.itemId())
                && oldWarehouseId.equals(request.warehouseId());

        long revisedOldBalance;

        if (sameLocation) {
            revisedOldBalance = oldBalance
                    - receipt.getQuantity()
                    + request.quantity();
        } else {
            revisedOldBalance = oldBalance
                    - receipt.getQuantity();
        }

        inventoryService.checkBalance(revisedOldBalance);

        receipt.setReceiptNumber(number);
        receipt.setItem(newItem);
        receipt.setWarehouse(newWarehouse);
        receipt.setQuantity(request.quantity());
        receipt.setReceiptDate(request.receiptDate());

        return toResponse(receiptRepository.save(receipt));
    }

    // DELETE receipt
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long id) {

        StockReceipt receipt = receiptRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Receipt not found"));

        Long itemId = receipt.getItem().getId();
        Long warehouseId = receipt.getWarehouse().getId();

        inventoryService.lockItem(itemId);

        long currentBalance = inventoryService.getBalance(
                itemId, warehouseId);

        long revisedBalance = currentBalance
                - receipt.getQuantity();

        inventoryService.checkBalance(revisedBalance);

        receiptRepository.delete(receipt);
    }

    // Find a valid, active warehouse
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

    // Check item status
    private void checkActiveItem(Item item) {

        if (!item.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Item is inactive");
        }
    }

    // Convert entity into JSON response DTO
    private StockReceiptResponse toResponse(StockReceipt receipt) {

        return new StockReceiptResponse(
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getItem().getId(),
                receipt.getWarehouse().getId(),
                receipt.getQuantity(),
                receipt.getReceiptDate()
        );
    }
}