package com.project.booking.kafka;

import com.project.booking.entity.SubFieldProjection;
import com.project.booking.entity.TimePriceRuleProjection;
import com.project.booking.entity.SubFieldClosureProjection;
import com.project.booking.entity.FieldOperatingHoursProjection;
import com.project.booking.entity.SubFieldOperatingHoursProjection;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.BookingTimePriceRuleProjectionRepository;
import com.project.booking.repository.FieldClosureProjectionRepository;
import com.project.booking.repository.FieldOperatingHoursProjectionRepository;
import com.project.booking.repository.SubFieldOperatingHoursProjectionRepository;
import com.project.common.events.field.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FieldProjectionConsumer {

    private final BookingSubFieldProjectionRepository subFieldRepository;
    private final BookingTimePriceRuleProjectionRepository timePriceRuleRepository;
    private final FieldOperatingHoursProjectionRepository fieldOperatingHoursRepository;
    private final SubFieldOperatingHoursProjectionRepository subFieldOperatingHoursRepository;
    private final FieldClosureProjectionRepository closureRepository;

    @KafkaListener(topics = FieldEventTopics.SUB_FIELD_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onSubFieldCreated(SubFieldCreatedEvent event) {
        upsertSubField(event.subFieldId(), event);
    }

    @KafkaListener(topics = FieldEventTopics.SUB_FIELD_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onSubFieldUpdated(SubFieldUpdatedEvent event) {
        upsertSubField(event.subFieldId(), event);
    }

    @KafkaListener(topics = FieldEventTopics.SUB_FIELD_DELETED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onSubFieldDeleted(SubFieldDeletedEvent event) {
        subFieldRepository.deleteById(event.subFieldId());
    }

    @KafkaListener(topics = FieldEventTopics.FIELD_OPERATING_HOURS_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onFieldOperatingHoursUpdated(FieldOperatingHoursUpdatedEvent event) {
        Map<DayOfWeek, FieldOperatingHoursProjection> existingByDay = fieldOperatingHoursRepository
                .findByFieldId(event.fieldId())
                .stream()
                .collect(Collectors.toMap(FieldOperatingHoursProjection::getDayOfWeek, Function.identity()));

        fieldOperatingHoursRepository.saveAll(event.operatingHours().stream()
                .map(hours -> {
                    FieldOperatingHoursProjection projection = existingByDay.getOrDefault(
                            hours.dayOfWeek(),
                            FieldOperatingHoursProjection.builder()
                                    .fieldId(event.fieldId())
                                    .dayOfWeek(hours.dayOfWeek())
                                    .build());
                    projection.setOpenTime(hours.openTime());
                    projection.setCloseTime(hours.closeTime());
                    projection.setClosed(Boolean.TRUE.equals(hours.closed()));
                    return projection;
                })
                .toList());
    }

    @KafkaListener(topics = FieldEventTopics.SUB_FIELD_OPERATING_HOURS_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onSubFieldOperatingHoursUpdated(SubFieldOperatingHoursUpdatedEvent event) {
        Map<DayOfWeek, SubFieldOperatingHoursProjection> existingByDay = subFieldOperatingHoursRepository
                .findBySubFieldId(event.subFieldId())
                .stream()
                .collect(Collectors.toMap(SubFieldOperatingHoursProjection::getDayOfWeek, Function.identity()));

        subFieldOperatingHoursRepository.saveAll(event.operatingHours().stream()
                .map(hours -> {
                    SubFieldOperatingHoursProjection projection = existingByDay.getOrDefault(
                            hours.dayOfWeek(),
                            SubFieldOperatingHoursProjection.builder()
                                    .subFieldId(event.subFieldId())
                                    .dayOfWeek(hours.dayOfWeek())
                                    .build());
                    projection.setOpenTime(hours.openTime());
                    projection.setCloseTime(hours.closeTime());
                    projection.setClosed(Boolean.TRUE.equals(hours.closed()));
                    return projection;
                })
                .toList());
    }

    @KafkaListener(topics = FieldEventTopics.FIELD_CLOSURE_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onClosureCreated(FieldClosureCreatedEvent event) {
        if (event.closures() == null || event.closures().isEmpty()) {
            return;
        }
        closureRepository.saveAll(event.closures().stream()
                .map(this::toClosureProjection)
                .toList());
    }

    @KafkaListener(topics = FieldEventTopics.FIELD_CLOSURE_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onClosureUpdated(FieldClosureUpdatedEvent event) {
        upsertClosure(event.closureId(), event.subFieldId(), event.startDate(), event.endDate(), event.reason());
    }

    @KafkaListener(topics = FieldEventTopics.FIELD_CLOSURE_DELETED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onClosureDeleted(FieldClosureDeletedEvent event) {
        closureRepository.deleteById(event.closureId());
    }

    private void upsertSubField(UUID id, SubFieldCreatedEvent event) {
        SubFieldProjection projection = subFieldRepository.findById(id)
                .orElseGet(() -> SubFieldProjection.builder().id(id).build());
        projection.setFieldId(event.fieldId());
        projection.setFieldName(event.fieldName());
        projection.setName(event.name());
        projection.setActive(Boolean.TRUE.equals(event.active()));
        projection.setOwnerId(event.ownerId());
        projection.setSubFieldType(event.subFieldType());
        projection.setMinimumBookingDurationMinutes(event.minimumBookingDurationMinutes());
        projection.setMaximumBookingDurationMinutes(event.maximumBookingDurationMinutes());
        projection.setBookingIntervalMinutes(event.bookingIntervalMinutes());
        subFieldRepository.save(projection);
        replaceTimePriceRules(id, event.timePriceRules());
    }

    private void upsertSubField(UUID id, SubFieldUpdatedEvent event) {
        SubFieldProjection projection = subFieldRepository.findById(id)
                .orElseGet(() -> SubFieldProjection.builder().id(id).build());
        projection.setFieldId(event.fieldId());
        projection.setFieldName(event.fieldName());
        projection.setName(event.name());
        projection.setActive(Boolean.TRUE.equals(event.active()));
        projection.setOwnerId(event.ownerId());
        projection.setSubFieldType(event.subFieldType());
        projection.setMinimumBookingDurationMinutes(event.minimumBookingDurationMinutes());
        projection.setMaximumBookingDurationMinutes(event.maximumBookingDurationMinutes());
        projection.setBookingIntervalMinutes(event.bookingIntervalMinutes());
        subFieldRepository.save(projection);
        replaceTimePriceRules(id, event.timePriceRules());
    }

    private void upsertClosure(UUID id, UUID subFieldId,
                           java.time.LocalDate startDate, java.time.LocalDate endDate, String reason) {
        SubFieldClosureProjection projection = closureRepository.findById(id)
                .orElseGet(() -> SubFieldClosureProjection.builder().id(id).build());
        projection.setSubFieldId(subFieldId);
        projection.setStartDate(startDate);
        projection.setEndDate(endDate);
        projection.setReason(reason);
        closureRepository.save(projection);
    }

    private SubFieldClosureProjection toClosureProjection(FieldClosureSnapshot closure) {
        return SubFieldClosureProjection.builder()
                .id(closure.closureId())
                .subFieldId(closure.subFieldId())
                .startDate(closure.startDate())
                .endDate(closure.endDate())
                .reason(closure.reason())
                .build();
    }

    private void replaceTimePriceRules(UUID subFieldId, List<TimePriceRuleSnapshot> rules) {
        timePriceRuleRepository.deleteBySubFieldId(subFieldId);
        if (rules == null || rules.isEmpty()) {
            return;
        }
        timePriceRuleRepository.saveAll(rules.stream()
                .map(rule -> TimePriceRuleProjection.builder()
                        .subFieldId(subFieldId)
                        .startTime(rule.startTime())
                        .endTime(rule.endTime())
                        .hourlyPrice(rule.hourlyPrice())
                        .build())
                .toList());
    }
}
