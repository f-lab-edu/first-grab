package com.firstgrab.global.exception;

public class InvalidOrderMessageException extends RuntimeException {

    public InvalidOrderMessageException(String orderId, int quantity) {
        super("유효하지 않은 주문 메시지입니다. orderId=" + orderId + ", quantity=" + quantity);
    }
}
