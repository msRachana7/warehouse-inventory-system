package warehouse_inventory.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.*;
import warehouse_inventory.entity.TransferStatus;

public record StockTransferRequest(

        @NotBlank
        String transferNumber,

        @NotNull
        Long itemId,

        @NotNull
        Long sourceWarehouseId,

        @NotNull
        Long destinationWarehouseId,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        LocalDate transferDate,

        @NotNull
        TransferStatus status

) {}