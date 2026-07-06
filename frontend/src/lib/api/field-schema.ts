import { z } from "zod";

const operatingHoursSchema = z.object({
  dayOfWeek: z.string(),
  openTime: z.string().optional(),
  closeTime: z.string().optional(),
  closed: z.boolean(),
});

export const fieldInputSchema = z.object({
  name: z.string().trim().min(2).max(100),
  description: z.string().max(2000).optional(),
  address: z.string().trim().min(1),
  ward: z.string().trim().min(1),
  wardCode: z.string().trim().min(1).max(20),
  province: z.string().trim().min(1),
  provinceCode: z.string().trim().min(1).max(20),
  legacyWard: z.string().trim().min(1),
  legacyWardCode: z.string().trim().min(1).max(20),
  legacyDistrict: z.string().trim().min(1),
  legacyProvince: z.string().trim().min(1),
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
  phoneNumber: z.string().trim().min(9).max(20),
  email: z.string().email().optional().or(z.literal("")),
  active: z.boolean(),
  operatingHours: z.array(operatingHoursSchema).length(7),
});
