package com.finventory.dto;

import com.finventory.model.LabelPrintJobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LabelPrintJobUpdateStatusRequest {
    @NotNull(message = "status is required")
    private LabelPrintJobStatus status;
}
