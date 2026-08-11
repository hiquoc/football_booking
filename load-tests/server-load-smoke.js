import http from "k6/http";
import { check, group, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const targetUrl = (__ENV.TARGET_URL || "http://localhost:8080").replace(/\/+$/, "");
const profile = (__ENV.TEST_PROFILE || "smoke").toLowerCase();
const authMode = (__ENV.AUTH_MODE || "dev-otp").toLowerCase();
const clientPhone = __ENV.CLIENT_PHONE || "0900000021";
const otpCode = __ENV.OTP_CODE || "111111";
const explicitFieldId = __ENV.FIELD_ID;
const explicitSubFieldId = __ENV.SUB_FIELD_ID;
const writeTraffic = (__ENV.WRITE_TRAFFIC || "false").toLowerCase() === "true";

export const options = {
  scenarios: {
    traffic: {
      executor: "ramping-vus",
      gracefulRampDown: "30s",
      stages: stagesFor(profile),
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1500"],
    checks: ["rate>0.95"],
  },
  summaryTrendStats: ["min", "avg", "med", "p(90)", "p(95)", "p(99)", "max"],
};

const businessErrors = new Counter("business_errors");
const authFailures = new Counter("auth_failures");
const successfulAvailabilityChecks = new Rate("successful_availability_checks");
const availabilityDuration = new Trend("availability_duration");

function stagesFor(selectedProfile) {
  if (selectedProfile === "vps") {
    return [
      { duration: "2m", target: Number(__ENV.RAMP_USERS || 25) },
      { duration: "5m", target: Number(__ENV.HOLD_USERS || 50) },
      { duration: "2m", target: Number(__ENV.PEAK_USERS || 100) },
      { duration: "5m", target: Number(__ENV.HOLD_USERS || 50) },
      { duration: "1m", target: 0 },
    ];
  }

  if (selectedProfile === "stress") {
    return [
      { duration: "2m", target: Number(__ENV.RAMP_USERS || 50) },
      { duration: "3m", target: Number(__ENV.HOLD_USERS || 100) },
      { duration: "3m", target: Number(__ENV.PEAK_USERS || 200) },
      { duration: "2m", target: 0 },
    ];
  }

  return [
    { duration: "30s", target: Number(__ENV.RAMP_USERS || 5) },
    { duration: "1m", target: Number(__ENV.HOLD_USERS || 10) },
    { duration: "30s", target: 0 },
  ];
}

export function setup() {
  const context = {
    token: null,
    fieldId: explicitFieldId || null,
    subFieldId: explicitSubFieldId || null,
  };

  if (authMode !== "none") {
    context.token = loginWithDevOtp();
  }

  const discovered = discoverFieldAndSubField(context.token);
  context.fieldId = context.fieldId || discovered.fieldId;
  context.subFieldId = context.subFieldId || discovered.subFieldId;

  if (!context.fieldId) {
    console.warn("No field ID discovered. Field detail calls will be skipped.");
  }

  if (!context.subFieldId) {
    console.warn("No sub-field ID discovered. Availability calls will be skipped.");
  }

  return context;
}

export default function (context) {
  const authHeaders = context.token ? { Authorization: `Bearer ${context.token}` } : {};
  const today = new Date();
  today.setDate(today.getDate() + (__VU % 14));
  const date = today.toISOString().slice(0, 10);

  group("public browsing", () => {
    const responses = http.batch([
      ["GET", `${targetUrl}/api/v1/fields/cards?page=${__ITER % 4}&size=12&sortBy=rating&direction=desc`, null, { tags: { name: "GET /api/v1/fields/cards" } }],
      ["GET", `${targetUrl}/api/v1/fields?page=0&size=12`, null, { tags: { name: "GET /api/v1/fields" } }],
      ["GET", `${targetUrl}/api/v1/field-types`, null, { tags: { name: "GET /api/v1/field-types" } }],
      ["GET", `${targetUrl}/api/v1/bookings/config`, null, { tags: { name: "GET /api/v1/bookings/config" } }],
      ["GET", `${targetUrl}/api/v1/community-posts?page=0&size=10`, null, { tags: { name: "GET /api/v1/community-posts" } }],
    ]);

    responses.forEach((response) => {
      check(response, {
        "public endpoint returned 2xx": (res) => res.status >= 200 && res.status < 300,
      }) || businessErrors.add(1);
    });
  });

  if (context.fieldId) {
    group("field detail", () => {
      const response = http.get(`${targetUrl}/api/v1/fields/${context.fieldId}/details`, {
        headers: authHeaders,
        tags: { name: "GET /api/v1/fields/{id}/details" },
      });

      check(response, {
        "field detail returned 2xx": (res) => res.status >= 200 && res.status < 300,
      }) || businessErrors.add(1);
    });
  }

  if (context.subFieldId) {
    group("availability", () => {
      const response = http.get(`${targetUrl}/api/v1/bookings/availability?subFieldId=${context.subFieldId}&date=${date}`, {
        tags: { name: "GET /api/v1/bookings/availability" },
      });

      availabilityDuration.add(response.timings.duration);
      const ok = check(response, {
        "availability returned 2xx": (res) => res.status >= 200 && res.status < 300,
      });
      successfulAvailabilityChecks.add(ok);
      if (!ok) {
        businessErrors.add(1);
      }
    });
  }

  if (context.token) {
    group("authenticated reads", () => {
      const responses = http.batch([
        ["GET", `${targetUrl}/api/v1/users/me`, null, { headers: authHeaders, tags: { name: "GET /api/v1/users/me" } }],
        ["GET", `${targetUrl}/api/v1/bookings/my?page=0&size=10`, null, { headers: authHeaders, tags: { name: "GET /api/v1/bookings/my" } }],
        ["GET", `${targetUrl}/api/v1/notifications/unread-count`, null, { headers: authHeaders, tags: { name: "GET /api/v1/notifications/unread-count" } }],
      ]);

      responses.forEach((response) => {
        check(response, {
          "authenticated endpoint returned 2xx": (res) => res.status >= 200 && res.status < 300,
        }) || authFailures.add(1);
      });
    });
  }

  if (writeTraffic && context.token && context.subFieldId) {
    group("optional booking writes", () => {
      const payload = JSON.stringify({
        subFieldId: context.subFieldId,
        bookingDate: date,
        startTime: `${String(8 + (__ITER % 8)).padStart(2, "0")}:00:00`,
        durationMinutes: 60,
        note: `k6 load test vu=${__VU} iter=${__ITER}`,
      });
      const response = http.post(`${targetUrl}/api/v1/bookings`, payload, {
        headers: { ...authHeaders, "Content-Type": "application/json" },
        tags: { name: "POST /api/v1/bookings" },
      });

      check(response, {
        "booking write returned expected status": (res) => [201, 400, 409].includes(res.status),
      }) || businessErrors.add(1);
    });
  }

  sleep(randomBetween(0.5, 2.5));
}

function loginWithDevOtp() {
  const sendResponse = http.post(
    `${targetUrl}/api/v1/auth/otp/send`,
    JSON.stringify({ phoneNumber: clientPhone }),
    { headers: { "Content-Type": "application/json" }, tags: { name: "POST /api/v1/auth/otp/send" } },
  );

  check(sendResponse, {
    "OTP send accepted": (res) => [200, 400, 429].includes(res.status),
  }) || authFailures.add(1);

  const verifyResponse = http.post(
    `${targetUrl}/api/v1/auth/otp/verify`,
    JSON.stringify({ phoneNumber: clientPhone, code: otpCode }),
    { headers: { "Content-Type": "application/json" }, tags: { name: "POST /api/v1/auth/otp/verify" } },
  );

  const verified = check(verifyResponse, {
    "OTP verify returned access token": (res) => res.status === 200 && Boolean(extractData(res)),
  });

  if (!verified) {
    authFailures.add(1);
    console.warn(`Authentication failed for ${clientPhone}; continuing unauthenticated. Status=${verifyResponse.status}`);
    return null;
  }

  return extractData(verifyResponse);
}

function discoverFieldAndSubField(token) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  const cardResponse = http.get(`${targetUrl}/api/v1/fields/cards?page=0&size=1`, {
    headers,
    tags: { name: "setup GET /api/v1/fields/cards" },
  });
  const cards = extractContent(cardResponse);
  const fieldId = explicitFieldId || cards[0]?.id || null;

  if (!fieldId) {
    return { fieldId: null, subFieldId: null };
  }

  const detailResponse = http.get(`${targetUrl}/api/v1/fields/${fieldId}/details`, {
    headers,
    tags: { name: "setup GET /api/v1/fields/{id}/details" },
  });
  const detail = extractData(detailResponse);
  const subFields = detail?.subFields || detail?.field?.subFields || [];

  return {
    fieldId,
    subFieldId: explicitSubFieldId || subFields[0]?.id || null,
  };
}

function extractContent(response) {
  const data = extractData(response);
  return data?.content || [];
}

function extractData(response) {
  try {
    const parsed = response.json();
    return parsed?.data;
  } catch {
    return null;
  }
}

function randomBetween(min, max) {
  return min + Math.random() * (max - min);
}
