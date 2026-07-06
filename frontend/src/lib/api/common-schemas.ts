import { z } from "zod";

// Java UUID and PostgreSQL UUID accept all 128-bit UUID values, including
// deterministic IDs whose version nibble is not one of the RFC versions.
export const uuidSchema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  "ID không đúng định dạng UUID",
);
