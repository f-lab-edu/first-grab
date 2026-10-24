package com.firstgrab.domain.stock;

public enum StockDecreaseResult {
    SUCCESS,
    SOLD_OUT,
    ALREADY_ORDERED,
    STOCK_NOT_FOUND;

    public static StockDecreaseResult fromCode(Long code) {
        validateNotNull(code);

        if (code >= 0) {
            return SUCCESS;
        }
        if (code == -1L) {
            return SOLD_OUT;
        }
        if (code == -2L) {
            return ALREADY_ORDERED;
        }
        if (code == -3L) {
            return STOCK_NOT_FOUND;
        }
        throw new IllegalStateException("알 수 없는 재고 차감 결과입니다. code=" + code);
    }

    private static void validateNotNull(Long code) {
        if (code == null) {
            throw new IllegalStateException("재고 차감 결과가 없습니다.");
        }
    }
}
