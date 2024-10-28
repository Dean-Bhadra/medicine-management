package com.debanjan.dto;

import lombok.Data;
import java.util.List;

@Data
public class SaleDTO {
    private String customerName;
    private String customerPhone;
    private String prescriptionNumber;
    private String paymentMethod;
    private List<SaleItemDTO> items;
}

