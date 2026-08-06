const HH_MM = /^([01]\d|2[0-3]):[0-5]\d$/;
const HH_MM_SS = /^([01]\d|2[0-3]):[0-5]\d:[0-5]\d$/;

export function toTimeInputValue(value: string | null | undefined, fallback = "00:00") {
  if (!value) return fallback;
  if (HH_MM.test(value)) return value;
  if (HH_MM_SS.test(value)) return value.slice(0, 5);
  return fallback;
}

export function toLocalTimePayload(value: string | null | undefined) {
  if (!value) return null;
  if (HH_MM.test(value)) return `${value}:00`;
  if (HH_MM_SS.test(value)) return value;
  return null;
}

export function requireLocalTimePayload(value: string | null | undefined, fallback = "00:00:00") {
  return toLocalTimePayload(value) ?? fallback;
}

export function toClosingTimeInputValue(value: string | null | undefined, fallback = "23:00") {
  return toTimeInputValue(value, fallback);
}

export function toClosingTimePayload(value: string | null | undefined, fallback = "23:00:00") {
  return requireLocalTimePayload(value, fallback);
}

export function closingTimeOptions(intervalMinutes = 30) {
  const options = clockTimeOptions(intervalMinutes);
  options.push({ value: "23:59", label: "12:00 CH" });
  return options;
}

export function clockTimeOptions(intervalMinutes = 30) {
  const options: Array<{ value: string; label: string }> = [];
  for (let minutes = 0; minutes < 24 * 60; minutes += intervalMinutes) {
    const hour = Math.floor(minutes / 60);
    const minute = minutes % 60;
    const value = `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
    options.push({ value, label: formatMeridiemTime(hour, minute) });
  }
  return options;
}

export function formatMeridiemTime(hour: number, minute: number) {
  const suffix = hour < 12 ? "SA" : "CH";
  const displayHour = hour === 0 ? 0 : hour % 12 || 12;
  return `${displayHour}:${String(minute).padStart(2, "0")} ${suffix}`;
}

export function formatTimeLabel(value: string | null | undefined, fallback = "") {
  const inputValue = toTimeInputValue(value, "");
  if (!inputValue) return fallback;
  if (inputValue === "23:59") return "12:00 CH";
  const [hour, minute] = inputValue.split(":").map(Number);
  return formatMeridiemTime(hour, minute);
}
