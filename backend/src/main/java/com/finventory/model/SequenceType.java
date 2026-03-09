package com.finventory.model;

public enum SequenceType {
    SALES_INVOICE("S"),
    PURCHASE_INVOICE("P"),
    SALES_RETURN("SR"),
    PURCHASE_RETURN("PR");

    private final String code;

    SequenceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
