package warehouse_inventory.dto;

import java.time.LocalDate;

public record StockIssueResponse(

        Long id,
        String issueNumber,
        Long itemId,
        Long warehouseId,
        int quantity,
        LocalDate issueDate

) {}
