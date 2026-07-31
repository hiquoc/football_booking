import { NextResponse } from "next/server";
import type { CommunityReport, PageResponse, PublicProfile } from "@/lib/api/types";
import { getCommunityReports } from "@/lib/server/community";
import { routeError } from "@/lib/server/route-response";
import { getPublicProfilesById } from "@/lib/server/users";

export async function GET(request: Request) {
  try {
    const query = new URL(request.url).searchParams;
    const page = Math.max(0, Number(query.get("page")) || 0);
    const size = Math.min(50, Math.max(1, Number(query.get("size")) || 20));
    const status = query.get("status") as "PENDING" | "REVIEWED" | null;
    const reports = await getCommunityReports(page, size, status ?? undefined);
    return NextResponse.json(await enrichReportUsers(reports));
  } catch (error) {
    return routeError(error);
  }
}

async function enrichReportUsers(reports: PageResponse<CommunityReport>) {
  const userIds = reports.content.flatMap((report) => [
    report.reporterId,
    report.post?.ownerId,
  ].filter((id): id is string => Boolean(id)));
  const profiles = await getPublicProfilesById(userIds);
  return {
    ...reports,
    content: reports.content.map((report) => {
      const reportedUserId = report.post?.ownerId ?? null;
      return {
        ...report,
        reporterDisplayName: displayName(profiles.get(report.reporterId), report.reporterId),
        reportedUserId,
        reportedDisplayName: reportedUserId
          ? displayName(profiles.get(reportedUserId), reportedUserId)
          : null,
      };
    }),
  };
}

function displayName(profile: PublicProfile | null | undefined, fallback: string) {
  return profile?.personal.fullName?.trim() || fallback;
}
