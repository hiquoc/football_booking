# Quy tắc tổ chức code Frontend

Tài liệu này là quy ước bắt buộc khi tạo mới hoặc chỉnh sửa code trong thư mục `frontend`.

## 1. Nguyên tắc chung

- Mỗi file chỉ nên có một trách nhiệm chính.
- Không gọi API trực tiếp trong component giao diện.
- Không đặt business logic trong `page.tsx` hoặc JSX.
- Browser chỉ gọi Next.js BFF qua các đường dẫn cùng origin `/api/**`.
- Chỉ code chạy trên server mới được gọi API gateway trực tiếp.
- Nội dung hiển thị cho người dùng phải dùng tiếng Việt.
- Query key phải được khai báo tập trung, không viết chuỗi query key rải rác.

## 2. Trách nhiệm theo thư mục

### `src/app/**/page.tsx`

Page chỉ chịu trách nhiệm:

- Đọc `params` và `searchParams`.
- Kiểm tra điều hướng như `notFound()` hoặc redirect.
- Prefetch dữ liệu cần cho lần render đầu tiên.
- Hydrate React Query cache.
- Ghép các component cấp trang.

Page không được:

- Gọi `fetch` trực tiếp.
- Chứa business logic.
- Chứa JSX giao diện quá lớn.
- Sử dụng Client Hook như `useQuery`, `useMutation` hoặc `useState`.

### `src/components/**`

Component chịu trách nhiệm hiển thị và tương tác giao diện.

- Component client lấy dữ liệu thông qua custom hook.
- Component không tự xây URL API hoặc gọi `fetch`.
- Tách component khi một khối có trách nhiệm riêng hoặc có thể tái sử dụng.
- Trạng thái loading, empty và error phải được xử lý rõ ràng.

Ví dụ:

```tsx
const { data, isPending, isError } = useFields(page);
```

### `src/lib/hooks/**`

Custom hook quản lý logic phía client:

- `useQuery` cho thao tác đọc dữ liệu.
- `useMutation` cho thao tác tạo, cập nhật, xóa hoặc hành động có side effect.
- Trạng thái query/mutation và invalidation cache.
- Kết hợp nhiều query liên quan thành hook cấp tính năng khi cần.

Hook không được gọi gateway trực tiếp. Hook phải sử dụng hàm trong `src/lib/client/**`.

### `src/lib/client/**`

Client API chịu trách nhiệm:

- Gọi BFF cùng origin `/api/**`.
- Cấu hình method, header và body.
- Parse response.
- Chuyển response lỗi thành `Error` nhất quán.
- Khai báo kiểu dữ liệu đầu vào và đầu ra.

Client API không chứa state React, JSX hoặc business logic giao diện.

### `src/app/api/**/route.ts`

Route Handler là lớp BFF giữa browser và gateway.

- Validate input từ browser.
- Kiểm tra origin cho request thay đổi dữ liệu.
- Gọi server data layer.
- Không trả access token hoặc refresh token cho browser.
- Chuyển lỗi backend thành HTTP response phù hợp.

### `src/lib/server/**`

Server data layer chịu trách nhiệm:

- Gọi API gateway trực tiếp.
- Quản lý token/session server-side.
- Cấu hình cache, revalidation và timeout.
- Prefetch React Query cache cho Server Component.
- Luôn sử dụng `import "server-only"` cho module chỉ chạy trên server.

### `src/lib/query-keys.ts`

Tất cả React Query key phải khai báo tại đây:

```ts
export const fieldQueryKeys = {
  all: ["fields"] as const,
  detail: (id: string) => ["fields", id] as const,
};
```

Server prefetch và client hook phải dùng cùng query key để hydration hoạt động đúng.

### `src/lib/api/**`

Chứa schema và type dùng chung:

- Kiểu request/response.
- Zod schema.
- API envelope và pagination type.

Không đặt request function hoặc React hook trong thư mục này.

## 3. Quy tắc React Query

### Query

Dùng `useQuery` cho dữ liệu chỉ đọc:

```ts
export function useField(id: string) {
  return useQuery({
    queryKey: fieldQueryKeys.detail(id),
    queryFn: () => fetchField(id),
  });
}
```

### Mutation

Dùng `useMutation` cho thao tác có side effect:

```ts
export function useUpdateProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateProfile,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: userQueryKeys.me }),
  });
}
```

- Không tự động retry booking, payment hoặc mutation không có idempotency.
- Sau mutation thành công, invalidate đúng query liên quan.
- Dùng `isPending`, `error` và `data` từ React Query thay vì tạo state trùng lặp.

## 4. Server prefetch và client hook

Server prefetch và client hook là hai lớp khác nhau:

- Server prefetch gọi gateway trực tiếp để tạo HTML và cache ban đầu.
- Client hook gọi BFF để quản lý cache, refetch và tương tác sau hydration.
- Không gọi Client Hook trong async Server Component.
- Có thể tách prefetch vào helper `server-only` để giữ `page.tsx` ngắn gọn.

Luồng chuẩn:

```text
Server Component
  -> server prefetch helper
  -> API gateway
  -> dehydrate cache
  -> HydrationBoundary
  -> Client Component
  -> custom React Query hook
  -> client API
  -> BFF route
```

## 5. Cấu trúc đề xuất cho một tính năng

```text
src/
├── app/
│   ├── (main)/fields/page.tsx
│   └── api/fields/route.ts
├── components/fields/
│   └── field-list-content.tsx
└── lib/
    ├── api/types.ts
    ├── client/fields.ts
    ├── hooks/use-fields.ts
    ├── server/fields.ts
    ├── server/field-query-cache.ts
    └── query-keys.ts
```

## 6. Checklist trước khi hoàn thành

- [ ] Page không gọi `fetch` trực tiếp và không chứa business logic.
- [ ] Component không tự xây request API.
- [ ] Client request nằm trong `lib/client`.
- [ ] Query và mutation nằm trong custom hook.
- [ ] Browser chỉ gọi `/api/**` cùng origin.
- [ ] Gateway chỉ được gọi từ server data layer.
- [ ] Query keys được khai báo tập trung.
- [ ] Mutation invalidate đúng cache liên quan.
- [ ] Không có state trùng với state đã được React Query cung cấp.
- [ ] Loading, empty và error state đã được xử lý.
- [ ] Nội dung giao diện sử dụng tiếng Việt.
- [ ] Typecheck, lint, test và production build đều đạt.
