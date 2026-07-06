import { z } from "zod";

export const phoneSchema = z
  .string()
  .trim()
  .regex(/^(0|\+84)[0-9]{9}$/, "Nhập số điện thoại Việt Nam hợp lệ");

export const sendOtpSchema = z.object({
  phoneNumber: phoneSchema,
});

export const verifyOtpSchema = z.object({
  phoneNumber: phoneSchema,
  code: z
    .string()
    .trim()
    .regex(/^\d{6}$/, "Mã OTP phải gồm 6 chữ số"),
});

export type SendOtpInput = z.infer<typeof sendOtpSchema>;
export type VerifyOtpInput = z.infer<typeof verifyOtpSchema>;
