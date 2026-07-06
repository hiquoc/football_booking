import "server-only";

import { Migration, v3 } from "vietnam-divisions-js";
import type { FieldInput } from "@/lib/api/types";
import { ApiError } from "./gateway";

export async function normalizeFieldLocation(
  input: FieldInput,
): Promise<FieldInput> {
  const mappings = await Migration.migrateWardCode(input.legacyWardCode);
  const selected = mappings.find(
    (mapping) =>
      mapping.newWardCode === input.wardCode &&
      mapping.newWardName === input.ward,
  );
  if (!selected) {
    throw new ApiError(
      "Địa chỉ hành chính v3 không khớp với địa chỉ cũ đã chọn",
      400,
      "INVALID_LOCATION_MAPPING",
    );
  }

  const provinces = await v3.getAllProvinces();
  const province = provinces.find(
    (item) =>
      item.name === selected.newProvinceName ||
      item.shortName === selected.newProvinceName,
  );
  if (!province) {
    throw new ApiError(
      "Không tìm thấy tỉnh/thành phố v3 tương ứng",
      400,
      "INVALID_LOCATION_MAPPING",
    );
  }

  return {
    ...input,
    ward: selected.newWardName,
    wardCode: selected.newWardCode,
    province: province.name,
    provinceCode: province.idProvince,
    legacyWard: selected.oldWardName,
    legacyDistrict: selected.oldDistrictName,
    legacyProvince: selected.oldProvinceName,
  };
}
