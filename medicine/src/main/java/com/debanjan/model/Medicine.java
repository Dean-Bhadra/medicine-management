package com.debanjan.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String manufacturer;

    @Column(nullable = false)
    private Double unitPrice;

    private Integer stockQuantity;

    private LocalDate expiryDate;

    @Column(nullable = false)
    private String batchNumber;

    private String description;

    private boolean requiresPrescription;
}