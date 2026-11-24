package com.debanjan.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sale_id")
    @JsonIgnore
    private Sale sale;

    @ManyToOne
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    private String medicineName;
    private String batchNumber;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}

