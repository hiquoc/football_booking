import { Mail, MapPin, Phone } from "lucide-react";
import type { Field } from "@/lib/api/types";
import { formatFieldAddress } from "@/lib/field-format";

export function FieldContactCard({ field }: { field: Field }) {
  const mapUrl = `https://www.openstreetmap.org/?mlat=${field.latitude}&mlon=${field.longitude}#map=17/${field.latitude}/${field.longitude}`;

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 text-slate-900 shadow-sm">
      <h2 className="text-xl font-black">Thông tin liên hệ</h2>
      <div className="mt-5 space-y-4 text-sm">
        <a
          href={`tel:${field.phoneNumber}`}
          className="flex items-center gap-3 text-slate-600 transition hover:text-green-700"
        >
          <Phone className="size-4" /> {field.phoneNumber}
        </a>
        {field.email ? (
          <a
            href={`mailto:${field.email}`}
            className="flex items-center gap-3 break-all text-slate-600 transition hover:text-green-700"
          >
            <Mail className="size-4" /> {field.email}
          </a>
        ) : null}
        <p className="flex items-start gap-3 text-slate-600">
          <MapPin className="mt-0.5 size-4 shrink-0" />{" "}
          {formatFieldAddress(field)}
        </p>
      </div>
      <a
        href={mapUrl}
        target="_blank"
        rel="noreferrer"
        className="mt-6 flex w-full justify-center rounded-xl bg-green-600 px-5 py-3 text-sm font-black text-white transition hover:bg-green-700"
      >
        Xem trên bản đồ
      </a>
    </section>
  );
}
