# Quy tắc tổ chức code Frontend

Tài liệu này là quy ước bắt buộc khi tạo mới hoặc chỉnh sửa code trong thư mục `frontend`.

## 1. Nguyên tắc chung

- Mỗi file chỉ nên có một trách nhiệm chính.
- Không gọi API trực tiếp trong component giao diện.
- Không đặt business logic trong `page.tsx` hoặc JSX.
- Browser chỉ gọi Next.js BFF qua các đường dẫn cùng origin `/api/**`.
- Chỉ code chạy trên server mới được gọi API gateway trực tiếp.
- Nội dung hiển thị cho người dùng phải dùng tiếng Việt.
- Khi thêm hoặc chỉnh sửa text hiển thị trên UI, viết bằng tiếng Việt có dấu. Không để lại label, heading, mô tả, nút, empty state, error state hoặc tooltip bằng tiếng Anh.
- Giá trị kỹ thuật từ API như enum/status/code có thể giữ nguyên trong dữ liệu, nhưng khi hiển thị cho người dùng phải map sang nhãn tiếng Việt.
- Query key phải được khai báo tập trung, không viết chuỗi query key rải rác.

## 1.1. Ngôn ngữ và giao diện

- Giao diện người dùng phải dùng tiếng Việt có dấu cho mọi nội dung hiển thị, bao gồm nút, nhãn, tiêu đề, mô tả, thông báo lỗi, trạng thái rỗng và tooltip.
- Theme chính của website là nền trắng/xám rất nhạt với xanh lá làm màu nhấn. Ưu tiên `bg-white`, `bg-slate-50`, `border-slate-200`, `text-slate-950`, `text-slate-600`, `bg-green-600`, `hover:bg-green-700`, `bg-green-50` và `text-green-700`.
- Dùng viền mảnh màu xám cho header, footer, card và panel chính. Dùng xanh lá cho CTA, icon nhấn, badge tích cực, trạng thái hover/focus và nút thao tác chính.
- Card nên có nền trắng, bo góc `rounded-2xl`, viền `border-slate-200`, bóng nhẹ và hover bằng viền xanh lá khi phù hợp.
- Tránh dùng xanh dương/sky làm màu chủ đạo trong UI mới. Nếu chỉnh component cũ đang dùng xanh dương, chuyển sang xanh lá trừ khi màu đó mang ý nghĩa nghiệp vụ riêng.
- Không dùng text tiếng Anh trong UI, trừ tên riêng, thương hiệu, mã kỹ thuật hoặc dữ liệu thô bắt buộc từ hệ thống.

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
- [ ] Nội dung giao diện sử dụng tiếng Việt có dấu, bao gồm label, heading, mô tả, nút, empty state, error state và tooltip.
- [ ] Enum/status/code từ API đã được map sang nhãn tiếng Việt trước khi hiển thị.
- [ ] Typecheck, lint, test và production build đều đạt.
# Bổ sung: Loading skeleton cho trang

- Mỗi page hoặc route detail được thêm mới/chỉnh sửa phải có `loading.tsx` hoặc skeleton trong component tương ứng.
- Skeleton phải mô phỏng bố cục thật của trang: header, card chính, sidebar, danh sách, form hoặc chart nếu có.
- Không dùng loading text đơn lẻ như "Đang tải..." cho page đã có bố cục rõ ràng.
- Skeleton dùng theme hiện tại: `bg-white`, `bg-slate-50`, `border-slate-200`, `rounded-2xl`, `animate-pulse`; tránh màu xanh dương/sky.
- Khi cập nhật UI của một page, kiểm tra lại loading, empty và error state của page đó trong cùng lượt chỉnh sửa.

# Bổ sung: Quy chuẩn theme và component tái sử dụng

- Layout trang chính dùng cùng chiều rộng với header, ưu tiên `mx-auto w-full max-w-[90rem] px-5 sm:px-8`.
- Header, footer, card, panel và form dùng viền xám (`border-slate-200`) trên nền trắng hoặc xám rất nhạt; không quay lại theme xanh dương cho các phần đã được chỉnh UI.
- Nút chính dùng nền xanh lá và chữ trắng. Nút phụ dùng nền trắng, viền xám và chữ slate. Nút nguy hiểm dùng rose nhưng vẫn giữ bố cục đơn giản, ít màu.
- Trạng thái booking dùng component `BookingStatusButton`; trạng thái bài cộng đồng và ứng tuyển dùng `CommunityPostStatusButton`. Không tự tạo badge trạng thái mới trong từng page.
- Cấp độ người chơi dùng component `SkillLevelButton` để có chiều cao, bo góc và màu nhất quán trên danh sách bài, chi tiết bài và ứng viên.
- Tên người dùng, tên sân và các thực thể có trang chi tiết phải là link chữ xanh lá (`text-green-700 hover:text-green-800`) thay vì thêm nút phụ như "Xem hồ sơ".
- Các enum như loại sân phải đi qua map dùng chung trong `src/lib/field-format.ts`; không viết lại nhãn tiếng Việt rải rác trong component.
- Khi component có loading bất đồng bộ, dùng skeleton đúng bố cục của component đó. Detail page cần có `loading.tsx` và client pending state cũng nên dùng cùng skeleton.
- Mọi text hiển thị mới phải là tiếng Việt có dấu, bao gồm fallback như "Người đăng", "Người chơi", "Chưa cập nhật" và thông báo lỗi.

# Bổ sung: Quy chuẩn dữ liệu nhạy cảm

- API không được trả số điện thoại hoặc email đầy đủ cho người không sở hữu dữ liệu đó.
- Nếu dữ liệu thuộc về requester hiện tại, endpoint phục vụ chỉnh sửa hồ sơ có thể trả dữ liệu đầy đủ khi thật sự cần thiết.
- Nếu dữ liệu không thuộc requester, số điện thoại và email phải được che một phần trước khi trả response. Ví dụ: số điện thoại chỉ lộ 3-4 số cuối, email chỉ lộ một phần nhỏ local-part và domain.
- UI chỉ hiển thị dữ liệu đã được backend che sẵn; không dựa vào frontend làm lớp bảo vệ duy nhất cho dữ liệu nhạy cảm.
- Với tên hiển thị người dùng, ưu tiên `fullName`. Nếu không có `fullName`, backend trả fallback như `User 1234` thay vì trả số điện thoại đầy đủ.
