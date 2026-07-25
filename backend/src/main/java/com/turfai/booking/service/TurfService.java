package com.turfai.booking.service;

import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import com.turfai.booking.exception.BaseException;
import com.turfai.booking.exception.ErrorCode;
import com.turfai.booking.repository.TurfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurfService {

    private final TurfRepository turfRepository;

    public Turf getTurfById(UUID turfId) {
        return turfRepository.findById(turfId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Turf not found with ID: " + turfId) {});
    }

    public Business getBusinessByTurfId(UUID turfId) {
        return getTurfById(turfId).getBusiness();
    }

    public List<Turf> getActiveTurfsByBusiness(UUID businessId) {
        return turfRepository.findByBusinessIdAndStatus(businessId, TurfStatus.ACTIVE);
    }
}
