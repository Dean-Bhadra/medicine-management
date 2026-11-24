package com.debanjan.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class SaleItemDTO {
    @NotNull @Positive
    private Long medicineId;
    @NotNull @Positive
    private Integer quantity;

    private java.math.BigDecimal expectedUnitPrice;
}
