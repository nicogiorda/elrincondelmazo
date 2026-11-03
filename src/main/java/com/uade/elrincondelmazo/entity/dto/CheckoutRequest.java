package com.uade.elrincondelmazo.entity.dto;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {

    public interface Confirmation extends jakarta.validation.groups.Default {}

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull(groups = Confirmation.class)
    @jakarta.validation.constraints.DecimalMin("0")
    private java.math.BigDecimal expectedTotal;

    @NotNull(groups = Confirmation.class)
    private java.util.UUID requestKey;

}
