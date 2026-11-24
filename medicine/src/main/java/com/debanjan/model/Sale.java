package com.debanjan.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data

public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime saleDate;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sale")
    private List<SaleItem> items;

    private String customerName;
    private String customerPhone;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(unique = true)
    private String checkoutKey;
    private String prescriptionNumber;
    private String paymentMethod;
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

}
