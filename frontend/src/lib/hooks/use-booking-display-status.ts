"use client";

import { useEffect, useState } from "react";
import type { Booking } from "@/lib/api/types";
import { deriveBookingDisplayStatus } from "@/lib/booking-format";

export function useBookingDisplayStatus(booking?: Booking) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  return booking ? deriveBookingDisplayStatus(booking, now) : null;
}
