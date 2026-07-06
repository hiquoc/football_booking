import { z } from "zod";
export const fieldTypeSchema = z.object({
  name: z.string().trim().min(2),
  defaultBookingDurationMinutes: z.number().int().positive(),
  description: z.string().trim().optional(),
  active: z.boolean(),
});
