package com.project.field.kafka;

import com.project.common.events.field.*;
import com.project.field.entity.BookingRule;
import com.project.field.entity.SubFieldClosure;
import com.project.field.entity.FieldOperatingHours;
import com.project.field.entity.SubField;
import com.project.field.entity.SubFieldOperatingHours;
import com.project.field.entity.TimePriceRule;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FieldEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSubFieldCreated(SubField subField) {
        kafkaTemplate.send(
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
        kafkaTemplate.send(
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
        kafkaTemplate.send(
                FieldEventTopics.SUB_FIELD_DELETED,
                subField.getId().toString(),
                new SubFieldDeletedEvent(subField.getId()));
    }

    public void publishFieldOperatingHoursUpdated(List<FieldOperatingHours> hours) {
        if (hours == null || hours.isEmpty()) {
            return;
        }
        FieldOperatingHours first = hours.getFirst();
        kafkaTemplate.send(
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
        kafkaTemplate.send(
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
        kafkaTemplate.send(
                FieldEventTopics.FIELD_CLOSURE_CREATED,
                closures.getFirst().getSubFieldId().toString(),
                new FieldClosureCreatedEvent(
                        java.util.UUID.randomUUID(),
                        closures.stream().map(this::toSnapshot).toList()));
    }

    public void publishClosureUpdated(SubFieldClosure closure) {
        kafkaTemplate.send(
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
        kafkaTemplate.send(
                FieldEventTopics.FIELD_CLOSURE_DELETED,
                closure.getId().toString(),
                new FieldClosureDeletedEvent(
                        closure.getId(),
                        closure.getSubFieldId()));
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
