package warehouse_inventory.dto;

public record DashboardResponse(

        long totalItems,
        long totalWarehouses,
        long lowStockItems,
        long totalStockQuantity,

        long totalReceipts,
        long totalIssues,
        long totalTransfers,

        long pendingTransfers,
        long completedTransfers,
        long cancelledTransfers,

        long totalReceivedUnits,
        long totalIssuedUnits,
        long totalTransferredUnits

) {}