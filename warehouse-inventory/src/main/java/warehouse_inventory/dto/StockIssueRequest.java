package warehouse_inventory.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record StockIssueRequest(

        @NotBlank
        String issueNumber,

        @NotNull
        Long itemId,

        @NotNull
        Long warehouseId,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        LocalDate issueDate

) {}