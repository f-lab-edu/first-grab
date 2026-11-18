package com.firstgrab.domain.stock;

public enum OrderReserveResult {
    SUCCESS,
    SOLD_OUT,
    LIMIT_EXCEEDED,
    STOCK_NOT_FOUND,
    NOT_ADMITTED,
    DUPLICATE_ORDER_ID;

    public static OrderReserveResult fromCode(Long code) {
        validateNotNull(code);

        if (code >= 0) {
            return SUCCESS;
        }
        if (code == -1L) {
            return SOLD_OUT;
        }
        if (code == -2L) {
            return LIMIT_EXCEEDED;
        }
        if (code == -3L) {
            return STOCK_NOT_FOUND;
        }
        if (code == -4L) {
            return NOT_ADMITTED;
        }
        if (code == -5L) {
            return DUPLICATE_ORDER_ID;
        }
        throw new IllegalStateException("알 수 없는 주문 예약 결과입니다. code=" + code);
    }

    private static void validateNotNull(Long code) {
        if (code == null) {
            throw new IllegalStateException("주문 예약 결과가 없습니다.");
        }
    }
}
