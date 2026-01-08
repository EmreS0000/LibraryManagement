package com.library.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "membership_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MembershipType extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull(message = "Max books is required")
    @Positive(message = "Max books must be positive")
    @Column(nullable = false)
    @JsonProperty("maxBorrowLimit")
    private Integer maxBooks;

    @NotNull(message = "Max loan days is required")
    @Positive(message = "Max loan days must be positive")
    @Column(nullable = false)
    @JsonProperty("loanDurationDays")
    private Integer maxLoanDays;
}
