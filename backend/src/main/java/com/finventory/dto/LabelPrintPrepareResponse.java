package com.finventory.dto;

import com.finventory.model.LabelBarcodeFormat;
import com.finventory.model.LabelPrintJobStatus;
import com.finventory.model.LabelTemplateName;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LabelPrintPrepareResponse {
    private UUID jobId;
    private LabelPrintJobStatus status;
    private LabelTemplateName templateName;
    private LabelBarcodeFormat requestedBarcodeFormat;
    private Boolean includeItemCode;
    private Integer totalLabelsRequested;
    private Integer totalLabelsValid;
    private List<LabelPrintPreparedItemDto> items;
    private List<LabelPrintInvalidItemDto> invalidItems;
}

