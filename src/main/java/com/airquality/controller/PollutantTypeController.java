package com.airquality.controller;

import com.airquality.dto.request.PollutantTypeRequest;
import com.airquality.dto.request.PollutantTypeUpdateRequest;
import com.airquality.dto.response.PollutantTypeResponse;
import com.airquality.service.PollutantTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pollutants")
public class PollutantTypeController {

    private final PollutantTypeService pollutantTypeService;

    public PollutantTypeController(PollutantTypeService pollutantTypeService) {
        this.pollutantTypeService = pollutantTypeService;
    }

    @PostMapping
    public ResponseEntity<PollutantTypeResponse> createPollutant(
            @Valid @RequestBody PollutantTypeRequest request) {
        PollutantTypeResponse response = pollutantTypeService.createPollutantType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PollutantTypeResponse>> getPollutants(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "20") int limit) {
        List<PollutantTypeResponse> pollutants = pollutantTypeService.getAllPollutantTypes(skip, limit);
        return ResponseEntity.ok(pollutants);
    }

    @GetMapping("/{pollutantId}")
    public ResponseEntity<PollutantTypeResponse> getPollutantById(@PathVariable Long pollutantId) {
        PollutantTypeResponse pollutant = pollutantTypeService.getPollutantTypeById(pollutantId);
        return ResponseEntity.ok(pollutant);
    }

    @PutMapping("/{pollutantId}")
    public ResponseEntity<PollutantTypeResponse> updatePollutant(
            @PathVariable Long pollutantId,
            @Valid @RequestBody PollutantTypeUpdateRequest request) {
        PollutantTypeResponse pollutant = pollutantTypeService.updatePollutantType(pollutantId, request);
        return ResponseEntity.ok(pollutant);
    }

    @DeleteMapping("/{pollutantId}")
    public ResponseEntity<Void> deletePollutant(@PathVariable Long pollutantId) {
        pollutantTypeService.deletePollutantType(pollutantId);
        return ResponseEntity.noContent().build();
    }
}
