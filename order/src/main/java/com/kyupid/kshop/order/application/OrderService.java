package com.kyupid.kshop.order.application;

import com.kyupid.kshop.order.application.exception.NoCancellationPermissionException;
import com.kyupid.kshop.order.application.exception.NoPermissionException;
import com.kyupid.kshop.order.application.exception.OrderNotFoundException;
import com.kyupid.kshop.order.application.exception.ValidationErrorException;
import com.kyupid.kshop.order.domain.*;
import com.kyupid.kshop.order.infra.StockAdjustment;
import com.kyupid.kshop.order.presentation.ChangeDeliveryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Order> getOrders(Long ordererMemberId) {
        return orderRepository.findAllByOrdererMemberId(ordererMemberId);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long memberId, Long orderId) {
        return orderRepository.findByOrdererMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public Order changeDeliveryInfo(ChangeDeliveryRequest request, Long orderId) {
        List<ValidationError> errors = validateOrderRequest(request);
        if (!errors.isEmpty()) throw new ValidationErrorException(errors);

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NoSuchElementException(orderId.toString()));
        if (!hasPermission(order.getOrdererMemberId(), request.getOrdererId())) {
            throw new NoPermissionException();
        }
        order.changeDeliveryInfo(request.getDeliveryInfo());
        return order;
    }

    private List<ValidationError> validateOrderRequest(ChangeDeliveryRequest request) {
        return new OrderRequestValidator().validate(request);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long memberId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!hasPermission(order.getOrdererMemberId(), memberId)) {
            throw new NoCancellationPermissionException();
        }
        order.cancel();
        increaseStock(order);
    }

    private void increaseStock(Order order) {
        List<OrderProduct> opList = order.getOrderProductList();
        List<StockAdjustment> saList = opList.stream()
                .map(StockAdjustment::from)
                .collect(Collectors.toList());
        productRepository.increaseStock(saList);
    }

    public boolean hasPermission(Long orderId, Long memberId) {
        return isRequesterOrderer(orderId, memberId) || isCurrentUserAdminRole();
    }

    private boolean isRequesterOrderer(Long orderId, Long memberId) {
        return orderId.equals(memberId);
    }

    private boolean isCurrentUserAdminRole() {
        /**
         * 현재 유저가 어드민인지 체크하는 로직을 삽입
         */
        return false;
    }
}
