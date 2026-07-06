"use client";

import { useMutation } from "@tanstack/react-query";
import { logout, sendOtp, verifyOtp } from "@/lib/client/auth";

export function useSendOtp() {
  return useMutation({ mutationFn: sendOtp });
}

export function useVerifyOtp() {
  return useMutation({ mutationFn: verifyOtp });
}

export function useLogout() {
  return useMutation({ mutationFn: logout });
}
