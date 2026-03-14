package com.finventory.dto;

import com.opencsv.bean.CsvBindByName;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ItemCsvDto {

    @CsvBindByName(column = "name", required = true)
    private String name;

    @CsvBindByName(column = "code", required = true)
    private String code;

    @CsvBindByName(column = "barcode")
    private String barcode;

    @CsvBindByName(column = "hsnCode")
    private String hsnCode;

    @CsvBindByName(column = "category")
    private String category;

    @CsvBindByName(column = "taxRate", required = true)
    private BigDecimal taxRate;

    @CsvBindByName(column = "unitPrice", required = true)
    private BigDecimal unitPrice;

    @CsvBindByName(column = "uom", required = true)
    private String uom;

    @CsvBindByName(column = "imageUrl")
    private String imageUrl;
}
