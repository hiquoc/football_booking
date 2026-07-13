package com.project.booking.kafka;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.entity.FieldOperatingHoursProjection;
import com.project.booking.entity.SubFieldClosureProjection;
import com.project.booking.entity.SubFieldOperatingHoursProjection;
import com.project.booking.entity.SubFieldProjection;
import com.project.booking.entity.TimePriceRuleProjection;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.BookingTimePriceRuleProjectionRepository;
import com.project.booking.repository.FieldClosureProjectionRepository;
import com.project.booking.repository.FieldOperatingHoursProjectionRepository;
import com.project.booking.repository.SubFieldOperatingHoursProjectionRepository;
import com.project.common.events.field.FieldClosureCreatedEvent;
import com.project.common.events.field.FieldClosureDeletedEvent;
import com.project.common.events.field.FieldClosureSnapshot;
import com.project.common.events.field.FieldClosureUpdatedEvent;
import com.project.common.events.field.FieldEventTopics;
import com.project.common.events.field.FieldOperatingHoursUpdatedEvent;
import com.project.common.events.field.SubFieldCreatedEvent;
import com.project.common.events.field.SubFieldDeletedEvent;
import com.project.common.events.field.SubFieldOperatingHoursUpdatedEvent;
import com.project.common.events.field.SubFieldUpdatedEvent;
import com.project.common.events.field.TimePriceRuleSnapshot;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FieldProjectionInboxEventHandler implements InboxEventHandler {

    private static final Set<String> TOPICS = Set.of(
            FieldEventTopics.SUB_FIELD_CREATED,
            FieldEventTopics.SUB_FIELD_UPDATED,
            FieldEventTopics.SUB_FIELD_DELETED,
            FieldEventTopics.FIELD_OPERATING_HOURS_UPDATED,
            FieldEventTopics.SUB_FIELD_OPERATING_HOURS_UPDATED,
            FieldEventTopics.FIELD_CLOSURE_CREATED,
            FieldEventTopics.FIELD_CLOSURE_UPDATED,
            FieldEventTopics.FIELD_CLOSURE_DELETED);

    private final InboxService inboxService;
    private final BookingSubFieldProjectionRepository subFieldRepository;
    private final BookingTimePriceRuleProjectionRepository timePriceRuleRepository;
    private final FieldOperatingHoursProjectionRepository fieldOperatingHoursRepository;
    private final SubFieldOperatingHoursProjectionRepository subFieldOperatingHoursRepository;
    private final FieldClosureProjectionRepository closureRepository;
    private final AvailabilityCacheService availabilityCacheService;

    @Override
    public boolean supports(String topic) {
        return TOPICS.contains(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        switch (event.getTopic()) {
            case FieldEventTopics.SUB_FIELD_CREATED ->
                    onSubFieldCreated(inboxService.payload(event, SubFieldCreatedEvent.class));
            case FieldEventTopics.SUB_FIELD_UPDATED ->
                    onSubFieldUpdated(inboxService.payload(event, SubFieldUpdatedEvent.class));
            case FieldEventTopics.SUB_FIELD_DELETED ->
                    onSubFieldDeleted(inboxService.payload(event, SubFieldDeletedEvent.class));
            case FieldEventTopics.FIELD_OPERATING_HOURS_UPDATED ->
                    onFieldOperatingHoursUpdated(inboxService.payload(event, FieldOperatingHoursUpdatedEvent.class));
            case FieldEventTopics.SUB_FIELD_OPERATING_HOURS_UPDATED ->
                    onSubFieldOperatingHoursUpdated(inboxService.payload(event, SubFieldOperatingHoursUpdatedEvent.class));
            case FieldEventTopics.FIELD_CLOSURE_CREATED ->
                    onClosureCreated(inboxService.payload(event, FieldClosureCreatedEvent.class));
            case FieldEventTopics.FIELD_CLOSURE_UPDATED ->
                    onClosureUpdated(inboxService.payload(event, FieldClosureUpdatedEvent.class));
            case FieldEventTopics.FIELD_CLOSURE_DELETED ->
                    onClosureDeleted(inboxService.payload(event, FieldClosureDeletedEvent.class));
            default -> throw new IllegalStateException("Unsupported topic " + event.getTopic());
        }
    }

    private void onSubFieldCreated(SubFieldCreatedEvent event) {
        upsertSubField(event.subFieldId(), event);
        availabilityCacheService.evictAll();
    }

    private void onSubFieldUpdated(SubFieldUpdatedEvent event) {
        upsertSubField(event.subFieldId(), event);
        availabilityCacheService.evictAll();
    }

    private void onSubFieldDeleted(SubFieldDeletedEvent event) {
        subFieldRepository.deleteById(event.subFieldId());
        availabilityCacheService.evictAll();
    }

    private void onFieldOperatingHoursUpdated(FieldOperatingHoursUpdatedEvent event) {
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
        availabilityCacheService.evictAll();
    }

    private void onSubFieldOperatingHoursUpdated(SubFieldOperatingHoursUpdatedEvent event) {
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
        availabilityCacheService.evictAll();
    }

    private void onClosureCreated(FieldClosureCreatedEvent event) {
        if (event.closures() == null || event.closures().isEmpty()) {
            return;
        }
        closureRepository.saveAll(event.closures().stream()
                .map(this::toClosureProjection)
                .toList());
        availabilityCacheService.evictAll();
    }

    private void onClosureUpdated(FieldClosureUpdatedEvent event) {
        upsertClosure(event.closureId(), event.subFieldId(), event.startDate(), event.endDate(), event.reason());
        availabilityCacheService.evictAll();
    }

    private void onClosureDeleted(FieldClosureDeletedEvent event) {
        closureRepository.deleteById(event.closureId());
        availabilityCacheService.evictAll();
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
