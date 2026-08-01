## Mandatory Rules

- Update Swagger/OpenAPI documentation whenever APIs change.
- Update DTO annotations when fields change.
- Update examples when validation changes.
- Update test whenever APIs change.
- No API task is complete without Swagger updates.
- Do not use Java `var`. Local variables must declare the concrete type so reviews and future changes do not depend on inference.
- Do not return full phone numbers or email addresses unless the data belongs to the requester and the endpoint needs full data for editing. For other users' data, mask the response server-side; phone numbers should expose only the last 3-4 digits and emails should expose only a small part of the local-part plus the domain.
- User display names should use `fullName` when available. If `fullName` is missing, return a masked fallback such as `User 1234`; never use a full phone number as display text.
