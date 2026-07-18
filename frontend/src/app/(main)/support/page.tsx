export default function SupportPage() {
  const email = process.env.NEXT_PUBLIC_SUPPORT_EMAIL ?? "support@example.com";
  const phone = process.env.NEXT_PUBLIC_SUPPORT_PHONE ?? "+84 000 000 000";

  return (
    <section className="mx-auto max-w-3xl px-4 py-12">
      <h1 className="text-3xl font-black text-slate-950">Platform Ban Information</h1>
      <div className="mt-6 space-y-4 text-sm leading-6 text-slate-600">
        <p>Your account may be restricted after repeated field bans or an approved payment dispute.</p>
        <p>To appeal, include your phone number, booking codes, and any supporting evidence.</p>
        <p className="font-semibold text-slate-900">Email: {email}</p>
        <p className="font-semibold text-slate-900">Phone: {phone}</p>
      </div>
    </section>
  );
}
