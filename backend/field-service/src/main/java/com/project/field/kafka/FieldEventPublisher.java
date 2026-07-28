package com.project.field.kafka;

import com.project.common.events.field.*;
import com.project.field.entity.BookingRule;
import com.project.field.entity.SubFieldClosure;
import com.project.field.entity.FieldOperatingHours;
import com.project.field.entity.SubField;
import com.project.field.entity.SubFieldOperatingHours;
import com.project.field.entity.TimePriceRule;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FieldEventPublisher {

    private final OutboxService outboxService;

    public void publishSubFieldCreated(SubField subField) {
        save("SubField", subField.getId().toString(),
                FieldEventTopics.SUB_FIELD_CREATED,
                subField.getId().toString(),
                new SubFieldCreatedEvent(
                        subField.getId(),
                        subField.getField().getId(),
                        subField.getField().getName(),
                        subField.getName(),
                        subField.getActive(),
                        subField.getField().getOwnerId(),
                        subField.getSubFieldType(),
                        minMinutes(subField.getBookingRule()),
                        maxMinutes(subField.getBookingRule()),
                        intervalMinutes(subField.getBookingRule()),
                        timePriceRules(subField)));
    }

    public void publishSubFieldUpdated(SubField subField) {
        save("SubField", subField.getId().toString(),
                FieldEventTopics.SUB_FIELD_UPDATED,
                subField.getId().toString(),
                new SubFieldUpdatedEvent(
                        subField.getId(),
                        subField.getField().getId(),
                        subField.getField().getName(),
                        subField.getName(),
                        subField.getActive(),
                        subField.getField().getOwnerId(),
                        subField.getSubFieldType(),
                        minMinutes(subField.getBookingRule()),
                        maxMinutes(subField.getBookingRule()),
                        intervalMinutes(subField.getBookingRule()),
                        timePriceRules(subField)));
    }

    public void publishSubFieldDeleted(SubField subField) {
        save("SubField", subField.getId().toString(),
                FieldEventTopics.SUB_FIELD_DELETED,
                subField.getId().toString(),
                new SubFieldDeletedEvent(subField.getId()));
    }

    public void publishFieldOperatingHoursUpdated(List<FieldOperatingHours> hours) {
        publishFieldOperatingHoursUpdated(List.of(), hours, List.of());
    }

    public void publishFieldOperatingHoursUpdated(List<FieldOperatingHours> previousHours, List<FieldOperatingHours> hours,
            List<UUID> affectedSubFieldIds) {
        if (hours == null || hours.isEmpty()) {
            return;
        }
        FieldOperatingHours first = hours.getFirst();
        save("FieldOperatingHours", first.getFieldId().toString(),
                FieldEventTopics.OPERATING_HOURS_CHANGED,
                first.getFieldId().toString(),
                new OperatingHoursChangedEvent(
                        "FIELD",
                        first.getFieldId(),
                        first.getFieldId(),
                        previousHours == null ? List.of() : previousHours.stream().map(this::toSnapshot).toList(),
                        hours.stream().map(this::toSnapshot).toList(),
                        Instant.now(),
                        UUID.randomUUID()));
    }

    public void publishSubFieldOperatingHoursUpdated(List<SubFieldOperatingHours> hours) {
        publishSubFieldOperatingHoursUpdated(List.of(), hours, null);
    }

    public void publishSubFieldOperatingHoursUpdated(List<SubFieldOperatingHours> previousHours,
            List<SubFieldOperatingHours> hours, UUID fieldId) {
        if (hours == null || hours.isEmpty()) {
            return;
        }
        SubFieldOperatingHours first = hours.getFirst();
        save("SubFieldOperatingHours", first.getSubFieldId().toString(),
                FieldEventTopics.OPERATING_HOURS_CHANGED,
                first.getSubFieldId().toString(),
                new OperatingHoursChangedEvent(
                        "SUBFIELD",
                        first.getSubFieldId(),
                        fieldId,
                        previousHours == null ? List.of() : previousHours.stream().map(this::toSnapshot).toList(),
                        hours.stream().map(this::toSnapshot).toList(),
                        Instant.now(),
                        UUID.randomUUID()));
    }

    public void publishTimePriceRulesChanged(SubField subField) {
        save("TimePriceRule", subField.getId().toString(),
                FieldEventTopics.TIME_PRICE_RULES_CHANGED,
                subField.getId().toString(),
                new TimePriceRulesChangedEvent(
                        subField.getId(),
                        subField.getField().getId(),
                        timePriceRules(subField),
                        Instant.now(),
                        UUID.randomUUID()));
    }

    public void publishClosureCreated(List<SubFieldClosure> closures) {
        if (closures == null || closures.isEmpty()) {
            return;
        }
        save("SubFieldClosure", closures.getFirst().getSubFieldId().toString(),
                FieldEventTopics.FIELD_CLOSURE_CREATED,
                closures.getFirst().getSubFieldId().toString(),
                new FieldClosureCreatedEvent(
                        java.util.UUID.randomUUID(),
                        closures.stream().map(this::toSnapshot).toList()));
    }

    public void publishClosureUpdated(SubFieldClosure closure) {
        save("SubFieldClosure", closure.getId().toString(),
                FieldEventTopics.FIELD_CLOSURE_UPDATED,
                closure.getId().toString(),
                new FieldClosureUpdatedEvent(
                        closure.getId(),
                        closure.getSubFieldId(),
                        closure.getStartDate(),
                        closure.getEndDate(),
                        closure.getReason()));
    }

    public void publishClosureDeleted(SubFieldClosure closure) {
        save("SubFieldClosure", closure.getId().toString(),
                FieldEventTopics.FIELD_CLOSURE_DELETED,
                closure.getId().toString(),
                new FieldClosureDeletedEvent(
                        closure.getId(),
                        closure.getSubFieldId()));
    }

    private void save(String aggregateType, String aggregateId, String topic, String key, Object payload) {
        outboxService.save(new OutboxSaveRequest(
                aggregateType,
                aggregateId,
                payload.getClass().getSimpleName(),
                topic,
                key,
                payload));
    }

    private Integer minMinutes(BookingRule bookingRule) {
        return bookingRule != null ? bookingRule.getMinimumBookingDurationMinutes() : null;
    }

    private Integer maxMinutes(BookingRule bookingRule) {
        return bookingRule != null ? bookingRule.getMaximumBookingDurationMinutes() : null;
    }

    private Integer intervalMinutes(BookingRule bookingRule) {
        return bookingRule != null ? bookingRule.getBookingIntervalMinutes() : null;
    }

    private List<TimePriceRuleSnapshot> timePriceRules(SubField subField) {
        if (subField.getTimePriceRules() == null) {
            return List.of();
        }
        return subField.getTimePriceRules().stream()
                .map(this::toSnapshot)
                .toList();
    }

    private TimePriceRuleSnapshot toSnapshot(TimePriceRule rule) {
        return new TimePriceRuleSnapshot(
                rule.getStartTime(),
                rule.getEndTime(),
                rule.getHourlyPrice());
    }

    private FieldClosureSnapshot toSnapshot(SubFieldClosure closure) {
        return new FieldClosureSnapshot(
                closure.getId(),
                closure.getSubFieldId(),
                closure.getStartDate(),
                closure.getEndDate(),
                closure.getReason());
    }

    private OperatingHoursSnapshot toSnapshot(FieldOperatingHours hours) {
        return new OperatingHoursSnapshot(
                hours.getDayOfWeek(),
                hours.getOpenTime(),
                hours.getCloseTime(),
                hours.getClosed(),
                hours.getOpen24Hours());
    }

    private OperatingHoursSnapshot toSnapshot(SubFieldOperatingHours hours) {
        return new OperatingHoursSnapshot(
                hours.getDayOfWeek(),
                hours.getOpenTime(),
                hours.getCloseTime(),
                hours.getClosed(),
                hours.getOpen24Hours());
    }
}
