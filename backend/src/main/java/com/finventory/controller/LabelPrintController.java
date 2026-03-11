package com.finventory.controller;

import com.finventory.dto.LabelPrintJobUpdateStatusRequest;
import com.finventory.dto.LabelPrintPrepareRequest;
import com.finventory.dto.LabelPrintPrepareResponse;
import com.finventory.model.LabelPrintJob;
import com.finventory.model.Role;
import com.finventory.repository.LabelPrintJobRepository;
import com.finventory.repository.UserRepository;
import com.finventory.service.LabelPrintService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labels/print-jobs")
@RequiredArgsConstructor
public class LabelPrintController {

    private final LabelPrintService labelPrintService;
    private final LabelPrintJobRepository labelPrintJobRepository;
    private final UserRepository userRepository;

    @PostMapping("/prepare")
    public ResponseEntity<LabelPrintPrepareResponse> prepare(
            @Valid @RequestBody LabelPrintPrepareRequest request, Principal principal) {
        return ResponseEntity.ok(labelPrintService.prepare(request, principal.getName()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody LabelPrintJobUpdateStatusRequest request,
            Principal principal) {
        LabelPrintJob job =
                labelPrintJobRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Print job not found"));

        var actor =
                userRepository
                        .findByUsername(principal.getName())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean allowed = actor.getRole() == Role.ADMIN || actor.getId().equals(job.getUser().getId());
        if (!allowed) {
            throw new IllegalArgumentException("Not allowed");
        }

        job.setStatus(request.getStatus());
        labelPrintJobRepository.save(job);
        return ResponseEntity.ok().build();
    }
}

