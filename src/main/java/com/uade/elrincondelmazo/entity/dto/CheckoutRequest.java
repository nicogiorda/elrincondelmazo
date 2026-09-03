package com.uade.elrincondelmazo.entity.dto;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotNull
    private PaymentMethod paymentMethod;

}
