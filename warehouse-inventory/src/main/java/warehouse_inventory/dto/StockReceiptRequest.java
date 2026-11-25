package warehouse_inventory.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record StockReceiptRequest(

        @NotBlank
        String receiptNumber,

        @NotNull
        Long itemId,

        @NotNull
        Long warehouseId,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        LocalDate receiptDate

) {}