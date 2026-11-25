package warehouse_inventory.dto;

public record InventoryBalanceResponse(

        Long itemId,
        String itemCode,
        String itemName,

        Long warehouseId,
        String warehouseName,

        long availableQuantity,
        int reorderLevel,
        boolean lowStock

) {}