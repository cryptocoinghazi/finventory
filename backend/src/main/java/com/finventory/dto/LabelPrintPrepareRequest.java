package com.finventory.dto;

import com.finventory.model.LabelBarcodeFormat;
import com.finventory.model.LabelTemplateName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LabelPrintPrepareRequest {
    @NotEmpty(message = "items is required")
    @Valid
    private List<LabelPrintPrepareLineRequest> items;

    @NotNull(message = "templateName is required")
    private LabelTemplateName templateName;

    @NotNull(message = "barcodeFormat is required")
    private LabelBarcodeFormat barcodeFormat;

    private Boolean includeItemCode;
}
