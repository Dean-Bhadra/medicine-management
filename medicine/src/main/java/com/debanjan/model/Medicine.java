package com.debanjan.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Data
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(columnDefinition = "bigint default 0", nullable = false)
    private Long version;

    @Column(nullable = false)
    @NotBlank @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String manufacturer;

    @Size(max = 255)
    private String supplier;

    @PositiveOrZero
    private Integer targetStock;

    @Column(nullable = false)
    @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2)
    private BigDecimal unitPrice;

    @NotNull @PositiveOrZero
    private Integer stockQuantity;

    @NotNull
    private LocalDate expiryDate;

    @Column(nullable = false)
    @NotBlank @Size(max = 255)
    private String batchNumber;

    private String description;

    private boolean requiresPrescription;
}