package com.firstgrab.global.exception;

public class OrderPublishException extends RuntimeException {

    public OrderPublishException(String orderId, Throwable cause) {
        super("주문 메시지 발행에 실패했습니다. orderId=" + orderId, cause);
    }
}
