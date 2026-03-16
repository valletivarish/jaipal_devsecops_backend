package com.airquality.service;

import com.airquality.dto.request.PollutantTypeRequest;
import com.airquality.dto.request.PollutantTypeUpdateRequest;
import com.airquality.dto.response.PollutantTypeResponse;
import com.airquality.exception.BadRequestException;
import com.airquality.exception.DuplicateResourceException;
import com.airquality.exception.ResourceNotFoundException;
import com.airquality.model.PollutantType;
import com.airquality.repository.PollutantTypeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PollutantTypeService {

    private final PollutantTypeRepository pollutantTypeRepository;

    public PollutantTypeService(PollutantTypeRepository pollutantTypeRepository) {
        this.pollutantTypeRepository = pollutantTypeRepository;
    }

    @Transactional
    public PollutantTypeResponse createPollutantType(PollutantTypeRequest request) {
        if (pollutantTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("PollutantType", "name", request.getName());
        }

        validateThresholds(request.getSafeThreshold(), request.getWarningThreshold(), request.getDangerThreshold());

        PollutantType pollutantType = new PollutantType();
        pollutantType.setName(request.getName());
        pollutantType.setDescription(request.getDescription());
        pollutantType.setUnit(request.getUnit());
        pollutantType.setSafeThreshold(request.getSafeThreshold());
        pollutantType.setWarningThreshold(request.getWarningThreshold());
        pollutantType.setDangerThreshold(request.getDangerThreshold());

        PollutantType saved = pollutantTypeRepository.save(pollutantType);
        return convertToResponse(saved);
    }

    public PollutantTypeResponse getPollutantTypeById(Long id) {
        PollutantType pollutantType = pollutantTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PollutantType", id));
        return convertToResponse(pollutantType);
    }

    public List<PollutantTypeResponse> getAllPollutantTypes(int skip, int limit) {
        int page = limit > 0 ? skip / limit : 0;
        Pageable pageable = PageRequest.of(page, limit);
        return pollutantTypeRepository.findAll(pageable).getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PollutantTypeResponse updatePollutantType(Long id, PollutantTypeUpdateRequest request) {
        PollutantType pollutantType = pollutantTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PollutantType", id));

        if (request.getName() != null) {
            pollutantType.setName(request.getName());
        }
        if (request.getDescription() != null) {
            pollutantType.setDescription(request.getDescription());
        }
        if (request.getUnit() != null) {
            pollutantType.setUnit(request.getUnit());
        }
        if (request.getSafeThreshold() != null) {
            pollutantType.setSafeThreshold(request.getSafeThreshold());
        }
        if (request.getWarningThreshold() != null) {
            pollutantType.setWarningThreshold(request.getWarningThreshold());
        }
        if (request.getDangerThreshold() != null) {
            pollutantType.setDangerThreshold(request.getDangerThreshold());
        }

        validateThresholds(
                pollutantType.getSafeThreshold(),
                pollutantType.getWarningThreshold(),
                pollutantType.getDangerThreshold()
        );

        PollutantType saved = pollutantTypeRepository.save(pollutantType);
        return convertToResponse(saved);
    }

    @Transactional
    public void deletePollutantType(Long id) {
        PollutantType pollutantType = pollutantTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PollutantType", id));
        pollutantTypeRepository.delete(pollutantType);
    }

    private void validateThresholds(Double safe, Double warning, Double danger) {
        if (safe >= warning) {
            throw new BadRequestException("Safe threshold must be less than warning threshold");
        }
        if (warning >= danger) {
            throw new BadRequestException("Warning threshold must be less than danger threshold");
        }
    }

    private PollutantTypeResponse convertToResponse(PollutantType pollutantType) {
        return PollutantTypeResponse.builder()
                .id(pollutantType.getId())
                .name(pollutantType.getName())
                .description(pollutantType.getDescription())
                .unit(pollutantType.getUnit())
                .safeThreshold(pollutantType.getSafeThreshold())
                .warningThreshold(pollutantType.getWarningThreshold())
                .dangerThreshold(pollutantType.getDangerThreshold())
                .createdAt(pollutantType.getCreatedAt())
                .updatedAt(pollutantType.getUpdatedAt())
                .build();
    }
}
