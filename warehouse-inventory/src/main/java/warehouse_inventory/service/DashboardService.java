package warehouse_inventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import warehouse_inventory.dto.*;
import warehouse_inventory.entity.*;
import warehouse_inventory.repository.*;

@Service
public class DashboardService {

    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockReceiptRepository receiptRepository;
    private final StockIssueRepository issueRepository;
    private final StockTransferRepository transferRepository;
    private final InventoryBalanceService balanceService;

    public DashboardService(
            ItemRepository itemRepository,
            WarehouseRepository warehouseRepository,
            StockReceiptRepository receiptRepository,
            StockIssueRepository issueRepository,
            StockTransferRepository transferRepository,
            InventoryBalanceService balanceService) {

        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.receiptRepository = receiptRepository;
        this.issueRepository = issueRepository;
        this.transferRepository = transferRepository;
        this.balanceService = balanceService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary() {

        // Count active items
        long totalItems = itemRepository.findAll()
                .stream()
                .filter(Item::isActive)
                .count();

        // Count active warehouses
        long totalWarehouses = warehouseRepository.findAll()
                .stream()
                .filter(Warehouse::isActive)
                .count();

        // Get stock balances
        List<InventoryBalanceResponse> balances =
                balanceService.getBalances(null);

        // Count distinct low-stock items
        long lowStockItems = balances.stream()
                .filter(InventoryBalanceResponse::lowStock)
                .map(InventoryBalanceResponse::itemId)
                .distinct()
                .count();

        // Total available units
        long totalStockQuantity = balances.stream()
                .mapToLong(
                    InventoryBalanceResponse::availableQuantity)
                .sum();

        // Get stock transaction records
        List<StockReceipt> receipts =
                receiptRepository.findAll();

        List<StockIssue> issues =
                issueRepository.findAll();

        List<StockTransfer> transfers =
                transferRepository.findAll();

        // Count transactions
        long totalReceipts = receipts.size();
        long totalIssues = issues.size();
        long totalTransfers = transfers.size();

        // Count transfers by status
        long pendingTransfers = transfers.stream()
                .filter(t -> t.getStatus()
                        == TransferStatus.PENDING)
                .count();

        long completedTransfers = transfers.stream()
                .filter(t -> t.getStatus()
                        == TransferStatus.COMPLETED)
                .count();

        long cancelledTransfers = transfers.stream()
                .filter(t -> t.getStatus()
                        == TransferStatus.CANCELLED)
                .count();

        // Calculate movement quantities
        long totalReceivedUnits = receipts.stream()
                .mapToLong(StockReceipt::getQuantity)
                .sum();

        long totalIssuedUnits = issues.stream()
                .mapToLong(StockIssue::getQuantity)
                .sum();

        long totalTransferredUnits = transfers.stream()
                .filter(t -> t.getStatus()
                        == TransferStatus.COMPLETED)
                .mapToLong(StockTransfer::getQuantity)
                .sum();

        return new DashboardResponse(
                totalItems,
                totalWarehouses,
                lowStockItems,
                totalStockQuantity,
                totalReceipts,
                totalIssues,
                totalTransfers,
                pendingTransfers,
                completedTransfers,
                cancelledTransfers,
                totalReceivedUnits,
                totalIssuedUnits,
                totalTransferredUnits
        );
    }
}