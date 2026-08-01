# UI restyle progress

Use this checklist when restyling pages so no route directory is skipped. Follow `CODE_STRUCTURE_RULES.md` for theme, reusable components, skeleton loading, Vietnamese text, green clickable entity names, and server-side masking for sensitive data.

## Completed in this pass

- [x] `app/(main)/fields` search dashboard: debounced keyword search, green filter controls.
- [x] `app/(main)/admin/page.tsx`: admin overview cards restyled.
- [x] `components/admin/admin-pagination.tsx`: shared admin pagination restyled.
- [x] `components/admin/admin-field-list.tsx`: field approval list restyled.
- [x] `components/admin/field-status-control.tsx`: green focus/loading state.
- [x] `app/(main)/admin/users` user list: Vietnamese labels, green links, debounced phone search preserved.
- [x] `app/(main)/admin/field-types`: page, loading state, and manager restyled.
- [x] `app/(main)/admin/payment-disputes`: page restyled with filters and green references.
- [x] `app/(main)/admin/recurring-bookings`: page wrapper and shared recurring list theme pass.
- [x] `app/(main)/admin/community-moderation`: page and moderation panel restyled with skeleton, post links, and reusable moderation status.
- [x] `app/(main)/notifications`: full notification page restored and list theme updated.
- [x] `app/(main)/recurring-bookings`: back navigation added and shared list retained.
- [x] `app/(main)/support`: Vietnamese support copy and theme layout applied.
- [x] `app/error.tsx` and `app/not-found.tsx`: neutral background and green action styling.
- [x] `app/(main)/owner`: owner dashboard, fields, bookings, recurring bookings, violations, banned clients, payment disputes, and field management components updated.

## Already updated earlier

- [x] Landing page
- [x] Header and footer
- [x] Login page
- [x] Contact page
- [x] Field list cards and field detail page
- [x] Field booking page
- [x] Booking list and booking detail pages
- [x] Community feed, create, detail, my posts, and my applications pages
- [x] Profile and public profile pages

## Remaining route directories

- [x] `app/(main)/admin/community-moderation`
- [x] `app/(main)/admin/fields` loading/page final QA
- [x] `app/(main)/admin/field-types`
- [x] `app/(main)/admin/payment-disputes`
- [x] `app/(main)/admin/recurring-bookings`
- [x] `app/(main)/admin/users`
- [x] `app/(main)/notifications`
- [x] `app/(main)/owner`
- [x] `app/(main)/recurring-bookings`
- [x] `app/(main)/support`
- [x] `app/error.tsx`
- [x] `app/not-found.tsx`
