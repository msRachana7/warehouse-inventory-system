package warehouse_inventory.dto;

import java.time.LocalDate;

public record StockReceiptResponse(

        Long id,
        String receiptNumber,
        Long itemId,
        Long warehouseId,
        int quantity,
        LocalDate receiptDate

) {}