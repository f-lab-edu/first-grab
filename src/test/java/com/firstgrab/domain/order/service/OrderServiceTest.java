package com.firstgrab.domain.order.service;

import com.firstgrab.domain.event.repository.EventRepository;
import com.firstgrab.domain.order.entity.Order;
import com.firstgrab.domain.order.entity.OrderStatus;
import com.firstgrab.domain.order.message.OrderRequested;
import com.firstgrab.domain.order.repository.OrderRepository;
import com.firstgrab.global.exception.InvalidOrderMessageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String ORDER_ID = "order-1";
    private static final Long EVENT_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final int QUANTITY = 2;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Test
    @DisplayName("주문 저장 성공")
    void placeOrderSuccess() {
        OrderRequested message = new OrderRequested(ORDER_ID, EVENT_ID, USER_ID, QUANTITY);
        when(orderRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(eventRepository.increaseSoldIfAvailable(EVENT_ID, QUANTITY)).thenReturn(1);

        OrderResult result = orderService.placeOrder(message);

        assertThat(result).isEqualTo(OrderResult.PENDING);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("재고 부족 시 실패 저장")
    void placeOrderSoldOut() {
        OrderRequested message = new OrderRequested(ORDER_ID, EVENT_ID, USER_ID, QUANTITY);
        when(orderRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(eventRepository.increaseSoldIfAvailable(EVENT_ID, QUANTITY)).thenReturn(0);

        OrderResult result = orderService.placeOrder(message);

        assertThat(result).isEqualTo(OrderResult.FAILED);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    @DisplayName("이미 저장된 주문")
    void placeOrderDuplicate() {
        OrderRequested message = new OrderRequested(ORDER_ID, EVENT_ID, USER_ID, QUANTITY);
        when(orderRepository.existsByOrderId(ORDER_ID)).thenReturn(true);

        OrderResult result = orderService.placeOrder(message);

        assertThat(result).isEqualTo(OrderResult.DUPLICATE);
        verify(eventRepository, never()).increaseSoldIfAvailable(anyLong(), anyInt());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("수량이 0 이하면 예외")
    void placeOrderInvalidQuantity() {
        OrderRequested message = new OrderRequested(ORDER_ID, EVENT_ID, USER_ID, 0);

        assertThatThrownBy(() -> orderService.placeOrder(message))
                .isInstanceOf(InvalidOrderMessageException.class);
        verifyNoInteractions(orderRepository, eventRepository);
    }
}
