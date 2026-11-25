package warehouse_inventory.dto;

import java.time.LocalDate;

import warehouse_inventory.entity.TransferStatus;

public record StockTransferResponse(

        Long id,
        String transferNumber,
        Long itemId,
        Long sourceWarehouseId,
        Long destinationWarehouseId,
        int quantity,
        LocalDate transferDate,
        TransferStatus status

) {}