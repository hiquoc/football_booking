package com.project.payment.dto;
import com.project.payment.enums.PaymentProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
public record CreateCheckoutRequest(
        UUID bookingId,
        @NotNull @DecimalMin(value="0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp="^[A-Za-z]{3}$") String currency,
        @Schema(defaultValue="STRIPE") PaymentProvider provider,
        @Size(max = 3000) String returnPath) {}
