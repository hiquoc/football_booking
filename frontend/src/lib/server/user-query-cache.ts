import "server-only";

import { QueryClient } from "@tanstack/react-query";
import { userQueryKeys } from "@/lib/query-keys";
import { getUsers } from "./users";

export async function prefetchUsers(page: number, size = 10) {
  const queryClient = new QueryClient();
  await queryClient.prefetchQuery({
    queryKey: userQueryKeys.list(page, size),
    queryFn: () => getUsers(page, size),
  });
  return queryClient;
}
