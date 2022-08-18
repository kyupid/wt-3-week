package com.kyupid.kshop.order.domain;

import com.kyupid.kshop.order.infra.ConfirmStockRequest;
import com.kyupid.kshop.order.infra.OrderProductInternalReqRes;

import java.util.List;

public interface ProductRepository {
    OrderProductInternalReqRes reserveStock(OrderProductInternalReqRes request);

    void confirmStock(ConfirmStockRequest confirmStockRequest);
}
