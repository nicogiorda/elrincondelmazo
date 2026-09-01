package com.uade.elrincondelmazo.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderPromotionResponse {

    //Mostramos el id de la promoción, el nombre y el descuento aplicado en el pedido, sin mostrar promotion completa.
    private Long promotionId;
    private String promotionName;
    private BigDecimal discountApplied;

}
