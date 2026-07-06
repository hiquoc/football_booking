import type { MutationSuccessResponse } from "@/lib/api/types";
import type { SendOtpInput, VerifyOtpInput } from "@/lib/api/auth-schemas";
import { jsonBody, requestJson } from "./http";

export function sendOtp(input: SendOtpInput) {
  return requestJson<MutationSuccessResponse>("/api/auth/otp/send", {
    method: "POST",
    ...jsonBody(input),
  });
}

export function verifyOtp(input: VerifyOtpInput) {
  return requestJson<MutationSuccessResponse>("/api/auth/otp/verify", {
    method: "POST",
    ...jsonBody(input),
  });
}

export function logout() {
  return requestJson<MutationSuccessResponse>("/api/auth/logout", {
    method: "POST",
  });
}
