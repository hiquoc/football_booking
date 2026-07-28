package com.project.booking.kafka;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.entity.FieldOperatingHoursProjection;
import com.project.booking.entity.SubFieldOperatingHoursProjection;
import com.project.booking.entity.TimePriceRuleProjection;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.BookingTimePriceRuleProjectionRepository;
import com.project.booking.repository.FieldClosureProjectionRepository;
import com.project.booking.repository.FieldOperatingHoursProjectionRepository;
import com.project.booking.repository.SubFieldOperatingHoursProjectionRepository;
import com.project.common.events.field.FieldEventTopics;
import com.project.common.events.field.OperatingHoursChangedEvent;
import com.project.common.events.field.OperatingHoursSnapshot;
import com.project.common.events.field.TimePriceRuleSnapshot;
import com.project.common.events.field.TimePriceRulesChangedEvent;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.service.InboxService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldProjectionInboxEventHandlerTest {

    @Mock
    private InboxService inboxService;

    @Mock
    private BookingSubFieldProjectionRepository subFieldRepository;

    @Mock
    private BookingTimePriceRuleProjectionRepository timePriceRuleRepository;

    @Mock
    private FieldOperatingHoursProjectionRepository fieldOperatingHoursRepository;

    @Mock
    private SubFieldOperatingHoursProjectionRepository subFieldOperatingHoursRepository;

    @Mock
    private FieldClosureProjectionRepository closureRepository;

    @Mock
    private AvailabilityCacheService availabilityCacheService;

    @Test
    void operatingHoursChangedUpdatesFieldProjectionOnly() {
        UUID fieldId = UUID.randomUUID();
        FieldProjectionInboxEventHandler handler = handler();
        InboxEvent envelope = envelope(FieldEventTopics.OPERATING_HOURS_CHANGED);
        OperatingHoursChangedEvent event = operatingHoursEvent(
                "FIELD",
                fieldId,
                fieldId,
                open(DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(23, 59)));
        when(inboxService.payload(envelope, OperatingHoursChangedEvent.class)).thenReturn(event);

        handler.handle(envelope);

        ArgumentCaptor<List<FieldOperatingHoursProjection>> captor = ArgumentCaptor.forClass(List.class);
        verify(fieldOperatingHoursRepository).saveAll(captor.capture());
        FieldOperatingHoursProjection saved = captor.getValue().getFirst();
        assertEquals(fieldId, saved.getFieldId());
        assertEquals(DayOfWeek.MONDAY, saved.getDayOfWeek());
        assertEquals(LocalTime.of(6, 0), saved.getOpenTime());
        assertEquals(LocalTime.of(23, 59), saved.getCloseTime());
        verify(timePriceRuleRepository, never()).saveAll(org.mockito.ArgumentMatchers.<List<TimePriceRuleProjection>>any());
    }

    @Test
    void operatingHoursChangedUpdatesSubFieldProjection() {
        UUID fieldId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        FieldProjectionInboxEventHandler handler = handler();
        InboxEvent envelope = envelope(FieldEventTopics.OPERATING_HOURS_CHANGED);
        OperatingHoursChangedEvent event = operatingHoursEvent(
                "SUBFIELD",
                subFieldId,
                fieldId,
                open(DayOfWeek.TUESDAY, LocalTime.of(18, 0), LocalTime.of(2, 0)));
        when(inboxService.payload(envelope, OperatingHoursChangedEvent.class)).thenReturn(event);

        handler.handle(envelope);

        ArgumentCaptor<List<SubFieldOperatingHoursProjection>> captor = ArgumentCaptor.forClass(List.class);
        verify(subFieldOperatingHoursRepository).saveAll(captor.capture());
        SubFieldOperatingHoursProjection saved = captor.getValue().getFirst();
        assertEquals(subFieldId, saved.getSubFieldId());
        assertEquals(DayOfWeek.TUESDAY, saved.getDayOfWeek());
        assertEquals(LocalTime.of(18, 0), saved.getOpenTime());
        assertEquals(LocalTime.of(2, 0), saved.getCloseTime());
    }

    @Test
    void timePriceRulesChangedReplacesProjectionFromFieldServiceSourceOfTruth() {
        UUID fieldId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        FieldProjectionInboxEventHandler handler = handler();
        InboxEvent envelope = envelope(FieldEventTopics.TIME_PRICE_RULES_CHANGED);
        TimePriceRulesChangedEvent event = new TimePriceRulesChangedEvent(
                subFieldId,
                fieldId,
                List.of(new TimePriceRuleSnapshot(
                        LocalTime.of(23, 0),
                        LocalTime.of(23, 59),
                        new BigDecimal("200000"))),
                Instant.EPOCH,
                UUID.randomUUID());
        when(inboxService.payload(envelope, TimePriceRulesChangedEvent.class)).thenReturn(event);

        handler.handle(envelope);

        verify(timePriceRuleRepository).deleteBySubFieldId(subFieldId);
        ArgumentCaptor<List<TimePriceRuleProjection>> captor = ArgumentCaptor.forClass(List.class);
        verify(timePriceRuleRepository).saveAll(captor.capture());
        TimePriceRuleProjection saved = captor.getValue().getFirst();
        assertEquals(subFieldId, saved.getSubFieldId());
        assertEquals(LocalTime.of(23, 0), saved.getStartTime());
        assertEquals(LocalTime.of(23, 59), saved.getEndTime());
        assertEquals(new BigDecimal("200000"), saved.getHourlyPrice());
    }

    @Test
    void fieldProjectionConsumerReceivesNewKafkaEvents() {
        InboxService inbox = mock(InboxService.class);
        FieldProjectionConsumer consumer = new FieldProjectionConsumer(inbox);
        ConsumerRecord<String, OperatingHoursChangedEvent> operatingHoursRecord = new ConsumerRecord<>(
                FieldEventTopics.OPERATING_HOURS_CHANGED,
                0,
                1L,
                UUID.randomUUID().toString(),
                operatingHoursEvent("FIELD", UUID.randomUUID(), UUID.randomUUID(),
                        open(DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(23, 59))));
        ConsumerRecord<String, TimePriceRulesChangedEvent> priceRulesRecord = new ConsumerRecord<>(
                FieldEventTopics.TIME_PRICE_RULES_CHANGED,
                0,
                2L,
                UUID.randomUUID().toString(),
                new TimePriceRulesChangedEvent(UUID.randomUUID(), UUID.randomUUID(), List.of(), Instant.EPOCH, UUID.randomUUID()));

        consumer.onOperatingHoursChanged(operatingHoursRecord);
        consumer.onTimePriceRulesChanged(priceRulesRecord);

        verify(inbox).receive(eq(operatingHoursRecord), org.mockito.ArgumentMatchers.isNull());
        verify(inbox).receive(eq(priceRulesRecord), org.mockito.ArgumentMatchers.isNull());
    }

    private FieldProjectionInboxEventHandler handler() {
        return new FieldProjectionInboxEventHandler(
                inboxService,
                subFieldRepository,
                timePriceRuleRepository,
                fieldOperatingHoursRepository,
                subFieldOperatingHoursRepository,
                closureRepository,
                availabilityCacheService);
    }

    private InboxEvent envelope(String topic) {
        return InboxEvent.builder().topic(topic).build();
    }

    private OperatingHoursChangedEvent operatingHoursEvent(String entityType, UUID entityId, UUID fieldId,
            OperatingHoursSnapshot hours) {
        return new OperatingHoursChangedEvent(
                entityType,
                entityId,
                fieldId,
                List.of(),
                List.of(hours),
                Instant.EPOCH,
                UUID.randomUUID());
    }

    private OperatingHoursSnapshot open(DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        return new OperatingHoursSnapshot(dayOfWeek, openTime, closeTime, false, false);
    }
}
