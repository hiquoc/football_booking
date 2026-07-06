import Link from "next/link";
import { Clock3, Images, Pencil, Shapes } from "lucide-react";
export function FieldManagementNav({ fieldId }: { fieldId: string }) {
  const root = `/owner/fields/${fieldId}`;
  const links = [
    { href: `${root}/edit`, label: "Thông tin", icon: Pencil },
    { href: `${root}/sub-fields`, label: "Sân con & giá", icon: Shapes },
    { href: `${root}/images`, label: "Hình ảnh", icon: Images },
    { href: `${root}/closures`, label: "Lịch đóng", icon: Clock3 },
  ];
  return (
    <nav className="mb-7 flex justify-center gap-2 overflow-x-auto">
      {links.map(({ href, label, icon: Icon }) => (
        <Link
          key={href}
          href={href}
          className="inline-flex shrink-0 items-center gap-2 rounded-full bg-sky-500 px-4 py-2 text-sm font-bold text-white hover:bg-sky-600 shadow-none"
        >
          <Icon className="size-4" /> {label}
        </Link>
      ))}
    </nav>
  );
}
