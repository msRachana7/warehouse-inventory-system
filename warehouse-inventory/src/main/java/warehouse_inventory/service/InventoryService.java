package warehouse_inventory.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import warehouse_inventory.entity.Item;
import warehouse_inventory.entity.TransferStatus;
import warehouse_inventory.repository.*;

@Service
public class InventoryService {

    private final ItemRepository itemRepository;
    private final StockReceiptRepository receiptRepository;
    private final StockIssueRepository issueRepository;
    private final StockTransferRepository transferRepository;

    public InventoryService(
            ItemRepository itemRepository,
            StockReceiptRepository receiptRepository,
            StockIssueRepository issueRepository,
            StockTransferRepository transferRepository) {

        this.itemRepository = itemRepository;
        this.receiptRepository = receiptRepository;
        this.issueRepository = issueRepository;
        this.transferRepository = transferRepository;
    }

    // Calculate stock for one item at one warehouse
    public long getBalance(Long itemId, Long warehouseId) {

        long received = receiptRepository.totalReceived(
                itemId, warehouseId);

        long issued = issueRepository.totalIssued(
                itemId, warehouseId);

        long incoming = transferRepository.totalIncoming(
                itemId, warehouseId, TransferStatus.COMPLETED);

        long outgoing = transferRepository.totalOutgoing(
                itemId, warehouseId, TransferStatus.COMPLETED);

        return received - issued + incoming - outgoing;
    }

    // Lock an item during a stock-changing transaction
    public Item lockItem(Long itemId) {

        return itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found"));
    }

    // Lock both items in a consistent order
    public void lockItems(Long firstId, Long secondId) {

        if (firstId.equals(secondId)) {
            lockItem(firstId);
            return;
        }

        lockItem(Math.min(firstId, secondId));
        lockItem(Math.max(firstId, secondId));
    }

    // Reject changes that would make stock negative
    public void checkBalance(long newBalance) {

        if (newBalance < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Operation rejected: insufficient stock");
        }
    }
}
