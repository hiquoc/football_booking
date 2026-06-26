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

import java.util.List;

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
        if (hours == null || hours.isEmpty()) {
            return;
        }
        FieldOperatingHours first = hours.getFirst();
        save("FieldOperatingHours", first.getFieldId().toString(),
                FieldEventTopics.FIELD_OPERATING_HOURS_UPDATED,
                first.getFieldId().toString(),
                new FieldOperatingHoursUpdatedEvent(
                        first.getFieldId(),
                        hours.stream().map(this::toSnapshot).toList()));
    }

    public void publishSubFieldOperatingHoursUpdated(List<SubFieldOperatingHours> hours) {
        if (hours == null || hours.isEmpty()) {
            return;
        }
        SubFieldOperatingHours first = hours.getFirst();
        save("SubFieldOperatingHours", first.getSubFieldId().toString(),
                FieldEventTopics.SUB_FIELD_OPERATING_HOURS_UPDATED,
                first.getSubFieldId().toString(),
                new SubFieldOperatingHoursUpdatedEvent(
                        first.getSubFieldId(),
                        hours.stream().map(this::toSnapshot).toList()));
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
                hours.getClosed());
    }

    private OperatingHoursSnapshot toSnapshot(SubFieldOperatingHours hours) {
        return new OperatingHoursSnapshot(
                hours.getDayOfWeek(),
                hours.getOpenTime(),
                hours.getCloseTime(),
                hours.getClosed());
    }
}
