package com.project.field.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.constants.GlobalConstants;
import com.project.common.dto.PageResponse;
import com.project.common.enums.SportType;
import com.project.common.enums.SubFieldType;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.GlobalExceptionHandler;
import com.project.common.security.HeaderAuthenticationFilter;
import com.project.field.config.SecurityConfig;
import com.project.field.dto.*;
import com.project.field.dto.response.SubFieldResponse;
import com.project.field.service.FieldScheduleService;
import com.project.field.service.FieldService;
import com.project.field.service.FieldImageUploadService;
import com.project.field.service.FieldTypeService;
import com.project.field.service.ReviewService;
import com.project.field.service.SubFieldService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        FieldController.class,
        SubFieldController.class,
        FieldImageController.class,
        FieldTypeController.class,
        ReviewController.class,
        InternalSubFieldController.class
})
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class, GlobalExceptionHandler.class})
class FieldServiceApiTest {

    private static final UUID USER_ID = UUID.fromString("b1e1c606-6b76-4154-af38-7dda890395ce");
    private static final UUID FIELD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SUB_FIELD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID CLOSURE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID REVIEW_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String INTERNAL_SECRET = "test-internal-gateway-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FieldService fieldService;

    @MockitoBean
    private FieldImageUploadService fieldImageUploadService;

    @MockitoBean
    private SubFieldService subFieldService;

    @MockitoBean
    private FieldScheduleService fieldScheduleService;

    @MockitoBean
    private FieldTypeService fieldTypeService;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void fieldCardSearchFiltersByProvinceCode() throws Exception {
        mockMvc.perform(get("/api/v1/fields/cards")
                        .param("provinceCode", "79")
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk());

        verify(fieldService).searchCards(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                eq("79"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                eq("rating"),
                eq("desc"),
                eq(0),
                eq(12),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void createFieldWithOwnerHeaderAcceptsWeeklyOperatingHours() throws Exception {
        when(fieldService.create(eq(USER_ID), org.mockito.ArgumentMatchers.any(FieldRequest.class))).thenReturn(fieldDto());

        mockMvc.perform(post("/api/v1/fields")
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fieldRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field created successfully"))
                .andExpect(jsonPath("$.data.id").value(FIELD_ID.toString()));

        ArgumentCaptor<FieldRequest> captor = ArgumentCaptor.forClass(FieldRequest.class);
        verify(fieldService).create(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getOperatingHours()).hasSize(7);
    }

    @Test
    void createFieldRejectsClientRole() throws Exception {
        mockMvc.perform(post("/api/v1/fields")
                        .headers(clientHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fieldRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFieldRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/fields")
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void updateFieldWithOwnerHeaderReturnsUpdatedField() throws Exception {
        when(fieldService.update(eq(FIELD_ID), eq(USER_ID), org.mockito.ArgumentMatchers.any(FieldRequest.class)))
                .thenReturn(fieldDto());

        mockMvc.perform(put("/api/v1/fields/{id}", FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fieldRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field updated successfully"));
    }

    @Test
    void getFieldAndListFieldsArePublic() throws Exception {
        when(fieldService.getWithDetailsById(eq(FIELD_ID), org.mockito.ArgumentMatchers.isNull())).thenReturn(fieldDto());
        when(fieldService.getAll(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull())).thenReturn(PageResponse.<FieldDto>builder()
                .content(List.of(fieldDto()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build());

        mockMvc.perform(get("/api/v1/fields/{id}", FIELD_ID)
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(FIELD_ID.toString()));

        when(fieldScheduleService.getFieldOperatingHours(FIELD_ID)).thenReturn(List.of(operatingHoursDto()));
        when(reviewService.getByFieldId(FIELD_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/fields/{id}/details", FIELD_ID)
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.field.id").value(FIELD_ID.toString()));

        mockMvc.perform(get("/api/v1/fields")
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(FIELD_ID.toString()));
    }

    @Test
    void getFieldDetailsPassesAuthenticatedOwnerToService() throws Exception {
        when(fieldService.getWithDetailsById(eq(FIELD_ID), org.mockito.ArgumentMatchers.any())).thenReturn(fieldDto());
        when(fieldScheduleService.getFieldOperatingHours(FIELD_ID)).thenReturn(List.of(operatingHoursDto()));
        when(reviewService.getByFieldId(FIELD_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/fields/{id}/details", FIELD_ID)
                        .headers(ownerHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.field.id").value(FIELD_ID.toString()));

        verify(fieldService).getWithDetailsById(
                eq(FIELD_ID),
                org.mockito.ArgumentMatchers.argThat(user ->
                        user != null && USER_ID.equals(user.id()) && "OWNER".equals(user.role())));
    }

    @Test
    void getFieldByIdPassesAuthenticatedOwnerToService() throws Exception {
        when(fieldService.getWithDetailsById(eq(FIELD_ID), org.mockito.ArgumentMatchers.any())).thenReturn(fieldDto());

        mockMvc.perform(get("/api/v1/fields/{id}", FIELD_ID)
                        .headers(ownerHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(FIELD_ID.toString()));

        verify(fieldService).getWithDetailsById(
                eq(FIELD_ID),
                org.mockito.ArgumentMatchers.argThat(user ->
                        user != null && USER_ID.equals(user.id()) && "OWNER".equals(user.role())));
    }

    @Test
    void getOwnerFieldsReturnsOnlyTheAuthenticatedOwnersPage() throws Exception {
        when(fieldService.getByOwnerId(eq(USER_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(PageResponse.<FieldDto>builder()
                        .content(List.of(fieldDto()))
                        .page(1)
                        .size(5)
                        .totalElements(6)
                        .totalPages(2)
                        .first(false)
                        .last(true)
                        .empty(false)
                        .build());

        mockMvc.perform(get("/api/v1/fields/owner?page=1&size=5&sort=createdAt,desc")
                        .headers(ownerHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(FIELD_ID.toString()))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5));

        ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(fieldService).getByOwnerId(eq(USER_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    void getOwnerFieldsRejectsNonOwnerRole() throws Exception {
        mockMvc.perform(get("/api/v1/fields/owner").headers(clientHeaders()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateFieldStatus() throws Exception {
        FieldDto approved = fieldDto();
        approved.setStatus(com.project.field.enums.FieldStatus.APPROVED);
        when(fieldService.updateStatus(FIELD_ID, com.project.field.enums.FieldStatus.APPROVED))
                .thenReturn(approved);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/fields/{id}/status", FIELD_ID)
                        .headers(adminHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void ownerCannotUpdateFieldStatus() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/fields/{id}/status", FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void fieldOperatingHoursEndpointsWorkWithOwnerHeaders() throws Exception {
        when(fieldScheduleService.getFieldOperatingHours(FIELD_ID)).thenReturn(List.of(operatingHoursDto()));
        when(fieldScheduleService.replaceFieldOperatingHours(
                eq(FIELD_ID), eq(USER_ID), eq("OWNER"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(operatingHoursDto()));

        mockMvc.perform(get("/api/v1/fields/{id}/operating-hours", FIELD_ID)
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dayOfWeek").value("MONDAY"));

        mockMvc.perform(put("/api/v1/fields/{id}/operating-hours", FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openWeek())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field operating hours updated successfully"));
    }

    @Test
    void replaceFieldOperatingHoursRejectsMissingHeader() throws Exception {
        mockMvc.perform(put("/api/v1/fields/{id}/operating-hours", FIELD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openWeek())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSubFieldUpdateDeleteAndListWork() throws Exception {
        when(subFieldService.create(eq(FIELD_ID), org.mockito.ArgumentMatchers.any(SubFieldRequest.class))).thenReturn(subFieldDto());
        when(subFieldService.update(eq(SUB_FIELD_ID), org.mockito.ArgumentMatchers.any(SubFieldRequest.class))).thenReturn(subFieldDto());
        when(subFieldService.getByFieldId(FIELD_ID)).thenReturn(List.of(subFieldDto()));

        mockMvc.perform(post("/api/v1/sub-fields/field/{fieldId}", FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subFieldRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sub-field created successfully"));

        mockMvc.perform(get("/api/v1/sub-fields/field/{fieldId}", FIELD_ID)
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(SUB_FIELD_ID.toString()));

        mockMvc.perform(put("/api/v1/sub-fields/{id}", SUB_FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subFieldRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sub-field updated successfully"));

        mockMvc.perform(delete("/api/v1/sub-fields/{id}", SUB_FIELD_ID)
                        .headers(ownerHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sub-field deleted successfully"));
    }

    @Test
    void createSubFieldRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/sub-fields/field/{fieldId}", FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void subFieldOperatingHoursAndClosureEndpointsWork() throws Exception {
        when(fieldScheduleService.getSubFieldOperatingHours(SUB_FIELD_ID)).thenReturn(List.of(operatingHoursDto()));
        when(fieldScheduleService.replaceSubFieldOperatingHours(
                eq(SUB_FIELD_ID), eq(USER_ID), eq("OWNER"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(operatingHoursDto()));
        when(fieldScheduleService.getClosures(SUB_FIELD_ID)).thenReturn(List.of(closureDto()));
        when(fieldScheduleService.createClosures(
                eq(USER_ID), eq("OWNER"), org.mockito.ArgumentMatchers.any(FieldClosureRequest.class)))
                .thenReturn(List.of(closureDto()));
        when(fieldScheduleService.updateClosure(
                eq(CLOSURE_ID), eq(USER_ID), eq("OWNER"), org.mockito.ArgumentMatchers.any(FieldClosureRequest.class)))
                .thenReturn(closureDto());

        mockMvc.perform(get("/api/v1/sub-fields/{id}/operating-hours", SUB_FIELD_ID)
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dayOfWeek").value("MONDAY"));

        mockMvc.perform(put("/api/v1/sub-fields/{id}/operating-hours", SUB_FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openWeek())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sub-fields/{id}/closures", SUB_FIELD_ID)
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(CLOSURE_ID.toString()));

        mockMvc.perform(post("/api/v1/sub-fields/closures")
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closureRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field closures created successfully"))
                .andExpect(jsonPath("$.data[0].subFieldId").value(SUB_FIELD_ID.toString()));

        mockMvc.perform(put("/api/v1/sub-fields/closures/{closureId}", CLOSURE_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closureRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field closure updated successfully"));

        mockMvc.perform(delete("/api/v1/sub-fields/closures/{closureId}", CLOSURE_ID)
                        .headers(ownerHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field closure deleted successfully"));
    }

    @Test
    void closureCreateReturnsBadRequestForBusinessError() throws Exception {
        when(fieldScheduleService.createClosures(
                eq(USER_ID), eq("OWNER"), org.mockito.ArgumentMatchers.any(FieldClosureRequest.class)))
                .thenThrow(new BadRequestException("Closure end date cannot be before start date"));

        mockMvc.perform(post("/api/v1/sub-fields/closures")
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closureRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Closure end date cannot be before start date"));
    }

    @Test
    void internalSubFieldEndpointIsPublic() throws Exception {
        when(subFieldService.getInternalSubFieldResponse(SUB_FIELD_ID)).thenReturn(SubFieldResponse.builder()
                .id(SUB_FIELD_ID)
                .fieldId(FIELD_ID)
                .name("Pitch A")
                .status("ACTIVE")
                .build());

        mockMvc.perform(get("/api/v1/internal/sub-fields/{subFieldId}", SUB_FIELD_ID)
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Fetched sub-field successfully"))
                .andExpect(jsonPath("$.data.id").value(SUB_FIELD_ID.toString()));
    }

    @Test
    void fieldImageEndpointsWorkWithOwnerHeaders() throws Exception {
        FieldImageDto image = FieldImageDto.builder()
                .id(10L)
                .imageUrl("https://example.com/10.jpg")
                .isPrimary(false)
                .displayOrder(0)
                .build();
        UUID requestId = UUID.randomUUID();
        ImageUploadSlotDto slot = new ImageUploadSlotDto(10L, "public-id", 123L, "signature",
                "api-key", "cloud", "https://api.cloudinary.com/v1_1/cloud/image/upload", false);
        when(fieldImageUploadService.issueSlots(eq(FIELD_ID), eq(USER_ID),
                org.mockito.ArgumentMatchers.any(ImageUploadSlotRequest.class))).thenReturn(List.of(slot));
        when(fieldImageUploadService.confirmBatch(eq(FIELD_ID), eq(USER_ID),
                org.mockito.ArgumentMatchers.any(ImageUploadBatchConfirmRequest.class))).thenReturn(List.of(image));
        when(fieldService.updateImageOrder(eq(FIELD_ID), org.mockito.ArgumentMatchers.any(FieldImageOrderRequest.class)))
                .thenReturn(List.of(image));

        mockMvc.perform(post("/api/v1/fields/{fieldId}/images/upload-slots", FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + requestId + "\",\"count\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Upload slots issued successfully"))
                .andExpect(jsonPath("$.data[0].publicId").value("public-id"));

        mockMvc.perform(post("/api/v1/fields/{fieldId}/images/confirm", FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uploads":[{"publicId":"public-id","secureUrl":"https://res.cloudinary.com/cloud/image/upload/v123/public-id.jpg",
                                 "version":123,"signature":"result-signature","format":"jpg","width":100,"height":100,"bytes":1000}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Image uploads confirmed successfully"));

        mockMvc.perform(put("/api/v1/fields/{fieldId}/images/order", FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(FieldImageOrderRequest.builder()
                                .imageIds(List.of(10L))
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Image order updated successfully"));

        mockMvc.perform(delete("/api/v1/fields/{fieldId}/images/{imageId}", FIELD_ID, 10L)
                        .headers(ownerHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Image deleted successfully"));
    }

    @Test
    void imageOrderRejectsInvalidBody() throws Exception {
        mockMvc.perform(put("/api/v1/fields/{fieldId}/images/order", FIELD_ID)
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fieldTypeAdminEndpointsAndPublicListWork() throws Exception {
        FieldTypeDto dto = FieldTypeDto.builder()
                .id(1L)
                .name(SportType.FOOTBALL)
                .allowedSubFieldTypes(List.of(SubFieldType.FOOTBALL_5V5))
                .defaultBookingDurationMinutes(60)
                .active(true)
                .build();
        when(fieldTypeService.create(org.mockito.ArgumentMatchers.any(FieldTypeRequest.class))).thenReturn(dto);
        when(fieldTypeService.update(eq(1L), org.mockito.ArgumentMatchers.any(FieldTypeRequest.class))).thenReturn(dto);
        when(fieldTypeService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(post("/api/v1/field-types")
                        .headers(adminHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fieldTypeRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field type created successfully"));

        mockMvc.perform(put("/api/v1/field-types/{id}", 1L)
                        .headers(adminHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fieldTypeRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field type updated successfully"));

        mockMvc.perform(delete("/api/v1/field-types/{id}", 1L)
                        .headers(adminHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field type deleted successfully"));

        mockMvc.perform(get("/api/v1/field-types")
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("FOOTBALL"));
    }

    @Test
    void fieldTypeAdminEndpointRejectsOwnerRole() throws Exception {
        mockMvc.perform(post("/api/v1/field-types")
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fieldTypeRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewEndpointsWorkWithClientHeaderAndPublicGet() throws Exception {
        ReviewDto dto = ReviewDto.builder()
                .id(REVIEW_ID)
                .fieldId(FIELD_ID)
                .userId(USER_ID)
                .rating(5)
                .comment("Great")
                .build();
        when(reviewService.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(ReviewRequest.class)))
                .thenReturn(dto);
        when(reviewService.getByFieldId(FIELD_ID)).thenReturn(List.of(dto));

        mockMvc.perform(post("/api/v1/reviews")
                        .headers(clientHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ReviewRequest.builder()
                                .fieldId(FIELD_ID)
                                .rating(5)
                                .comment("Great")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Review submitted successfully"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()));

        mockMvc.perform(get("/api/v1/reviews/field/{fieldId}", FIELD_ID)
                        .header(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(REVIEW_ID.toString()));
    }

    @Test
    void reviewCreateRejectsOwnerRole() throws Exception {
        mockMvc.perform(post("/api/v1/reviews")
                        .headers(ownerHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ReviewRequest.builder()
                                .fieldId(FIELD_ID)
                                .rating(5)
                                .build())))
                .andExpect(status().isForbidden());
    }

    private FieldRequest fieldRequest() {
        return FieldRequest.builder()
                .name("ABC Football Center")
                .address("123 Nguyen Hue")
                .ward("Phuong Sai Gon")
                .wardCode("26743")
                .province("Thanh pho Ho Chi Minh")
                .provinceCode("79")
                .legacyWard("Phuong Ben Nghe")
                .legacyWardCode("26743")
                .legacyDistrict("Quan 1")
                .legacyProvince("Thanh pho Ho Chi Minh")
                .latitude(new BigDecimal("10.776900"))
                .longitude(new BigDecimal("106.700900"))
                .phoneNumber("0862470050")
                .email("abc@football.vn")
                .active(true)
                .operatingHours(openWeek())
                .build();
    }

    private FieldDto fieldDto() {
        return FieldDto.builder()
                .id(FIELD_ID)
                .ownerId(USER_ID)
                .name("ABC Football Center")
                .address("123 Nguyen Hue")
                .ward("Phuong Sai Gon")
                .wardCode("26743")
                .province("Thanh pho Ho Chi Minh")
                .provinceCode("79")
                .legacyWard("Phuong Ben Nghe")
                .legacyWardCode("26743")
                .legacyDistrict("Quan 1")
                .legacyProvince("Thanh pho Ho Chi Minh")
                .latitude(new BigDecimal("10.776900"))
                .longitude(new BigDecimal("106.700900"))
                .phoneNumber("0862470050")
                .active(true)
                .build();
    }

    private SubFieldRequest subFieldRequest() {
        return SubFieldRequest.builder()
                .name("Pitch A")
                .subFieldType(SubFieldType.FOOTBALL_5V5)
                .active(true)
                .timePriceRules(timePriceRules())
                .build();
    }

    private SubFieldDto subFieldDto() {
        return SubFieldDto.builder()
                .id(SUB_FIELD_ID)
                .fieldId(FIELD_ID)
                .name("Pitch A")
                .subFieldType(SubFieldType.FOOTBALL_5V5)
                .active(true)
                .timePriceRules(timePriceRules())
                .build();
    }

    private List<TimePriceRuleDto> timePriceRules() {
        return List.of(TimePriceRuleDto.builder()
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(23, 0))
                .hourlyPrice(new BigDecimal("200000"))
                .build());
    }

    private List<OperatingHoursRequest> openWeek() {
        return List.of(
                hours(DayOfWeek.MONDAY),
                hours(DayOfWeek.TUESDAY),
                hours(DayOfWeek.WEDNESDAY),
                hours(DayOfWeek.THURSDAY),
                hours(DayOfWeek.FRIDAY),
                hours(DayOfWeek.SATURDAY),
                OperatingHoursRequest.builder().dayOfWeek(DayOfWeek.SUNDAY).closed(true).build()
        );
    }

    private OperatingHoursRequest hours(DayOfWeek dayOfWeek) {
        return OperatingHoursRequest.builder()
                .dayOfWeek(dayOfWeek)
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(23, 0))
                .closed(false)
                .build();
    }

    private OperatingHoursDto operatingHoursDto() {
        return OperatingHoursDto.builder()
                .fieldId(FIELD_ID)
                .subFieldId(SUB_FIELD_ID)
                .dayOfWeek(DayOfWeek.MONDAY)
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(23, 0))
                .closed(false)
                .build();
    }

    private FieldClosureRequest closureRequest() {
        return FieldClosureRequest.builder()
                .subFieldIds(List.of(SUB_FIELD_ID))
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .reason("Maintenance")
                .build();
    }

    private FieldClosureDto closureDto() {
        return FieldClosureDto.builder()
                .id(CLOSURE_ID)
                .subFieldId(SUB_FIELD_ID)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .reason("Maintenance")
                .build();
    }

    private FieldTypeRequest fieldTypeRequest() {
        return FieldTypeRequest.builder()
                .name(SportType.FOOTBALL)
                .defaultBookingDurationMinutes(60)
                .description("Football")
                .active(true)
                .build();
    }

    private org.springframework.http.HttpHeaders clientHeaders() {
        return headers("CLIENT");
    }

    private org.springframework.http.HttpHeaders ownerHeaders() {
        return headers("OWNER");
    }

    private org.springframework.http.HttpHeaders adminHeaders() {
        return headers("ADMIN");
    }

    private org.springframework.http.HttpHeaders headers(String role) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(GlobalConstants.HEADER_INTERNAL_SECRET, INTERNAL_SECRET);
        headers.add(GlobalConstants.HEADER_USER_ID, USER_ID.toString());
        headers.add(GlobalConstants.HEADER_USER_ROLE, role);
        headers.add(GlobalConstants.HEADER_USER_EMAIL, "api-test@example.com");
        return headers;
    }
}
