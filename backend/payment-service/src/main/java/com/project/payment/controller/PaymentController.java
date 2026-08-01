package com.project.payment.controller;
import com.project.common.dto.ApiResponse;
import com.project.common.security.*;
import com.project.payment.dto.*;
import com.project.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @Operation(summary="Create a provider checkout session")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE','OWNER')") @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@CurrentUser UserPrincipal user,
            @Valid @RequestBody CreateCheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Checkout session created", paymentService.createCheckout(user.id(), request)));
    }
    @Operation(summary="Get payment status for a booking")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE','OWNER')") @GetMapping("/{bookingId}")
    public ApiResponse<PaymentResponse> status(@CurrentUser UserPrincipal user, @PathVariable UUID bookingId) {
        return ApiResponse.success(paymentService.getByBookingId(user.id(), bookingId));
    }
    @Operation(summary="Receive a signed Stripe webhook")
    @PostMapping(value="/webhook", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
            @RequestHeader("Stripe-Signature") String signature) {
        paymentService.processWebhook(rawBody, signature); return ResponseEntity.ok().build();
    }
}
