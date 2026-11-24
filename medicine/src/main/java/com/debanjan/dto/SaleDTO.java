package com.debanjan.dto;

import jakarta.validation.Valid;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.util.List;

@Data
public class SaleDTO {
    @Size(max = 64)
    private String checkoutKey;
    private String customerName;
    private String customerPhone;
    private String prescriptionNumber;
    private String paymentMethod;
    @NotEmpty
    private List<@NotNull @Valid SaleItemDTO> items;
}

