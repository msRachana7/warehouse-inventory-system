package warehouse_inventory.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import warehouse_inventory.dto.*;
import warehouse_inventory.entity.*;
import warehouse_inventory.repository.*;

@Service
public class StockTransferService {

    private final StockTransferRepository transferRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    public StockTransferService(
            StockTransferRepository transferRepository,
            WarehouseRepository warehouseRepository,
            InventoryService inventoryService) {

        this.transferRepository = transferRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
    }

    // GET all transfers
    @Transactional(readOnly = true)
    public List<StockTransferResponse> getAll() {

        return transferRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // GET transfer by ID
    @Transactional(readOnly = true)
    public StockTransferResponse getById(Long id) {

        StockTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transfer not found"));

        return toResponse(transfer);
    }

    // POST create transfer
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StockTransferResponse create(
            StockTransferRequest request) {

        validateLocations(request);

        if (request.status() == TransferStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A new transfer cannot be CANCELLED");
        }

        Item item = inventoryService.lockItem(request.itemId());
        checkActiveItem(item);

        Warehouse source = getActiveWarehouse(
                request.sourceWarehouseId());

        Warehouse destination = getActiveWarehouse(
                request.destinationWarehouseId());

        String number = request.transferNumber().trim();

        if (transferRepository.existsByTransferNumberIgnoreCase(number)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transfer number already exists");
        }

        // Check stock if completing immediately
        checkRevisedBalances(null, request);

        StockTransfer transfer = new StockTransfer();

        transfer.setTransferNumber(number);
        transfer.setItem(item);
        transfer.setSourceWarehouse(source);
        transfer.setDestinationWarehouse(destination);
        transfer.setQuantity(request.quantity());
        transfer.setTransferDate(request.transferDate());
        transfer.setStatus(request.status());

        return toResponse(transferRepository.save(transfer));
    }

    // PUT update transfer
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StockTransferResponse update(
            Long id, StockTransferRequest request) {

        StockTransfer transfer = transferRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transfer not found"));

        if (transfer.getStatus() == TransferStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A cancelled transfer cannot be edited");
        }

        validateLocations(request);

        // Do not move a completed transfer back to PENDING
        if (transfer.getStatus() == TransferStatus.COMPLETED
                && request.status() == TransferStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Completed transfer cannot become PENDING");
        }

        String number = request.transferNumber().trim();

        if (transferRepository
                .existsByTransferNumberIgnoreCaseAndIdNot(
                        number, id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transfer number already exists");
        }

        // Lock affected items
        inventoryService.lockItems(
                transfer.getItem().getId(),
                request.itemId());

        // Cancellation only changes the status.
        // Original transfer details must stay unchanged.
        if (request.status() == TransferStatus.CANCELLED) {

            if (!sameDetails(transfer, request)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Keep original details when cancelling");
            }

            // Reverse the old completed transfer safely
            checkRevisedBalances(transfer, null);

            transfer.setStatus(TransferStatus.CANCELLED);

            return toResponse(transferRepository.save(transfer));
        }

        Item item = inventoryService.lockItem(request.itemId());
        checkActiveItem(item);

        Warehouse source = getActiveWarehouse(
                request.sourceWarehouseId());

        Warehouse destination = getActiveWarehouse(
                request.destinationWarehouseId());

        // Reverse previous effects and preview new effects
        checkRevisedBalances(transfer, request);

        transfer.setTransferNumber(number);
        transfer.setItem(item);
        transfer.setSourceWarehouse(source);
        transfer.setDestinationWarehouse(destination);
        transfer.setQuantity(request.quantity());
        transfer.setTransferDate(request.transferDate());
        transfer.setStatus(request.status());

        return toResponse(transferRepository.save(transfer));
    }

    // DELETE transfer
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long id) {

        StockTransfer transfer = transferRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transfer not found"));

        inventoryService.lockItem(transfer.getItem().getId());

        // If completed, verify that removing the
        // incoming stock won't cause negative inventory.
        checkRevisedBalances(transfer, null);

        transferRepository.delete(transfer);
    }

    // Represents one item's stock in one warehouse
    private record StockKey(Long itemId, Long warehouseId) {}

    // Check the final stock after reversing/applying
    // transfer effects, before changing the database.
    private void checkRevisedBalances(
            StockTransfer oldTransfer,
            StockTransferRequest newRequest) {

        Map<StockKey, Long> changes = new HashMap<>();

        // Reverse the previous completed transfer
        if (oldTransfer != null
                && oldTransfer.getStatus() == TransferStatus.COMPLETED) {

            Long itemId = oldTransfer.getItem().getId();
            long quantity = oldTransfer.getQuantity();

            addChange(changes,
                    itemId,
                    oldTransfer.getSourceWarehouse().getId(),
                    quantity);

            addChange(changes,
                    itemId,
                    oldTransfer.getDestinationWarehouse().getId(),
                    -quantity);
        }

        // Apply the replacement completed transfer
        if (newRequest != null
                && newRequest.status() == TransferStatus.COMPLETED) {

            addChange(changes,
                    newRequest.itemId(),
                    newRequest.sourceWarehouseId(),
                    -newRequest.quantity());

            addChange(changes,
                    newRequest.itemId(),
                    newRequest.destinationWarehouseId(),
                    newRequest.quantity());
        }

        // Every affected item/warehouse must stay >= 0
        for (Map.Entry<StockKey, Long> entry : changes.entrySet()) {

            StockKey key = entry.getKey();

            long current = inventoryService.getBalance(
                    key.itemId(), key.warehouseId());

            long revised = current + entry.getValue();

            inventoryService.checkBalance(revised);
        }
    }

    // Accumulate changes for each item/warehouse
    private void addChange(
            Map<StockKey, Long> changes,
            Long itemId,
            Long warehouseId,
            long quantityChange) {

        StockKey key = new StockKey(itemId, warehouseId);

        changes.merge(key, quantityChange, Long::sum);
    }

    // Validate different warehouses
    private void validateLocations(StockTransferRequest request) {

        if (request.sourceWarehouseId()
                .equals(request.destinationWarehouseId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source and destination must be different");
        }
    }

    // When cancelling, don't change original details
    private boolean sameDetails(
            StockTransfer transfer,
            StockTransferRequest request) {

        return Objects.equals(
                    transfer.getTransferNumber(),
                    request.transferNumber().trim())
                && Objects.equals(
                    transfer.getItem().getId(),
                    request.itemId())
                && Objects.equals(
                    transfer.getSourceWarehouse().getId(),
                    request.sourceWarehouseId())
                && Objects.equals(
                    transfer.getDestinationWarehouse().getId(),
                    request.destinationWarehouseId())
                && transfer.getQuantity() == request.quantity()
                && Objects.equals(
                    transfer.getTransferDate(),
                    request.transferDate());
    }

    // Find active warehouse
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

    // Validate item
    private void checkActiveItem(Item item) {

        if (!item.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Item is inactive");
        }
    }

    // Convert entity into JSON response
    private StockTransferResponse toResponse(
            StockTransfer transfer) {

        return new StockTransferResponse(
                transfer.getId(),
                transfer.getTransferNumber(),
                transfer.getItem().getId(),
                transfer.getSourceWarehouse().getId(),
                transfer.getDestinationWarehouse().getId(),
                transfer.getQuantity(),
                transfer.getTransferDate(),
                transfer.getStatus()
        );
    }
}