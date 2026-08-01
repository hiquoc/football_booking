"use client";

import { useEffect, useMemo, useState } from "react";
import { Districts, Migration, Provinces, v3 } from "vietnam-divisions-js";
import type { Commune } from "vietnam-divisions-js/communes";
import type { District } from "vietnam-divisions-js/districts";
import type { WardMapping } from "vietnam-divisions-js/migration";
import type { Province } from "vietnam-divisions-js/provinces";
import type { ProvinceV3 } from "vietnam-divisions-js/v3";
import type { FieldLocationValue } from "./field-location-types";

export function VietnamLocationFields({
  value,
  onChange,
}: {
  value: FieldLocationValue;
  onChange: (value: FieldLocationValue) => void;
}) {
  const [provinces, setProvinces] = useState<Province[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [wards, setWards] = useState<Commune[]>([]);
  const [v3Provinces, setV3Provinces] = useState<ProvinceV3[]>([]);
  const [migrationOptions, setMigrationOptions] = useState<WardMapping[]>([]);
  const [migrationError, setMigrationError] = useState<string | null>(null);
  const [isMigrating, setIsMigrating] = useState(false);

  const legacyProvinceCode = useMemo(
    () =>
      provinces.find((item) => item.name === value.legacyProvince)?.idProvince,
    [provinces, value.legacyProvince],
  );
  const legacyDistrictCode = useMemo(
    () =>
      districts.find((item) => item.name === value.legacyDistrict)?.idDistrict,
    [districts, value.legacyDistrict],
  );
  const visibleDistricts = legacyProvinceCode
    ? districts.filter((item) => item.idProvince === legacyProvinceCode)
    : [];
  const visibleWards = legacyDistrictCode
    ? wards.filter((item) => item.idDistrict === legacyDistrictCode)
    : [];

  useEffect(() => {
    let active = true;
    Promise.all([Provinces.getAllProvincesSorted(), v3.getAllProvinces()]).then(
      ([legacyItems, currentItems]) => {
        if (!active) return;
        setProvinces(legacyItems);
        setV3Provinces(currentItems);
      },
    );
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    if (!legacyProvinceCode) {
      return () => {
        active = false;
      };
    }
    Provinces.getDistrictsByProvinceId(legacyProvinceCode).then((items) => {
      if (active) setDistricts(items);
    });
    return () => {
      active = false;
    };
  }, [legacyProvinceCode]);

  useEffect(() => {
    let active = true;
    if (!legacyDistrictCode) {
      return () => {
        active = false;
      };
    }
    Districts.getCommunesByDistrictId(legacyDistrictCode).then((items) => {
      if (active) setWards(items);
    });
    return () => {
      active = false;
    };
  }, [legacyDistrictCode]);

  async function applyMapping(
    mapping: WardMapping,
    baseValue: FieldLocationValue,
  ) {
    const currentProvinces = v3Provinces.length
      ? v3Provinces
      : await v3.getAllProvinces();
    const province = currentProvinces.find(
      (item) =>
        item.name === mapping.newProvinceName ||
        item.shortName === mapping.newProvinceName,
    );
    if (!province) {
      setMigrationError("Không tìm thấy mã tỉnh/thành phố sau sáp nhập tương ứng.");
      return;
    }
    setMigrationError(null);
    onChange({
      ...baseValue,
      ward: mapping.newWardName,
      wardCode: mapping.newWardCode,
      province: province.name,
      provinceCode: province.idProvince,
    });
  }

  async function selectLegacyWard(legacyWardCode: string) {
    const selectedWard = visibleWards.find(
      (item) => item.idCommune === legacyWardCode,
    );
    const nextValue: FieldLocationValue = {
      ...value,
      legacyWard: selectedWard?.name ?? "",
      legacyWardCode,
      ward: "",
      wardCode: "",
      province: "",
      provinceCode: "",
    };
    onChange(nextValue);
    setMigrationOptions([]);
    setMigrationError(null);
    if (!legacyWardCode) return;

    setIsMigrating(true);
    try {
      const mappings = await Migration.migrateWardCode(legacyWardCode);
      setMigrationOptions(mappings);
      if (mappings.length === 0) {
        setMigrationError("Không tìm thấy địa chỉ v3 tương ứng với phường/xã đã chọn.");
      } else if (mappings.length === 1) {
        await applyMapping(mappings[0], nextValue);
      }
    } catch {
      setMigrationError("Không thể chuyển đổi địa chỉ sang dữ liệu v3.");
    } finally {
      setIsMigrating(false);
    }
  }

  const selectedMappingKey = migrationOptions.find(
    (mapping) =>
      mapping.newWardCode === value.wardCode &&
      mapping.newProvinceName === value.province,
  )
    ? `${value.province}|${value.wardCode}`
    : "";

  return (
    <div className="grid gap-5 sm:grid-cols-2">
      <div className="sm:col-span-2">
        <LocationLabel label="Địa chỉ">
          <input
            required
            name="address"
            value={value.address}
            onChange={(event) =>
              onChange({ ...value, address: event.target.value })
            }
            className="input-field"
            placeholder="Số nhà, tên đường"
          />
        </LocationLabel>
      </div>

      <LocationLabel label="Tỉnh/Thành phố (địa chỉ cũ)">
        <select
          required
          value={value.legacyProvince}
          onChange={(event) => {
            onChange({
              ...value,
              legacyProvince: event.target.value,
              legacyDistrict: "",
              legacyWard: "",
              legacyWardCode: "",
              province: "",
              provinceCode: "",
              ward: "",
              wardCode: "",
            });
            setMigrationOptions([]);
            setMigrationError(null);
          }}
          className="input-field"
        >
          <option value="">Chọn tỉnh/thành phố</option>
          {provinces.map((province) => (
            <option key={province.idProvince} value={province.name}>
              {province.name}
            </option>
          ))}
        </select>
      </LocationLabel>

      <LocationLabel label="Quận/Huyện/Thị xã/Thành phố (địa chỉ cũ)">
        <select
          required
          value={value.legacyDistrict}
          disabled={!legacyProvinceCode}
          onChange={(event) => {
            onChange({
              ...value,
              legacyDistrict: event.target.value,
              legacyWard: "",
              legacyWardCode: "",
              province: "",
              provinceCode: "",
              ward: "",
              wardCode: "",
            });
            setMigrationOptions([]);
            setMigrationError(null);
          }}
          className="input-field disabled:cursor-not-allowed disabled:opacity-60"
        >
          <option value="">Chọn đơn vị cấp huyện</option>
          {visibleDistricts.map((district) => (
            <option key={district.idDistrict} value={district.name}>
              {district.name}
            </option>
          ))}
        </select>
      </LocationLabel>

      <div className="sm:col-span-2">
        <LocationLabel label="Phường/Xã (địa chỉ cũ)">
          <select
            required
            value={value.legacyWardCode}
            disabled={!legacyDistrictCode || isMigrating}
            onChange={(event) => void selectLegacyWard(event.target.value)}
            className="input-field disabled:cursor-not-allowed disabled:opacity-60"
          >
            <option value="">
              {isMigrating ? "Đang chuyển đổi địa chỉ…" : "Chọn phường/xã"}
            </option>
            {visibleWards.map((ward) => (
              <option key={ward.idCommune} value={ward.idCommune}>
                {ward.name}
              </option>
            ))}
          </select>
        </LocationLabel>
      </div>

      {migrationOptions.length > 1 ? (
        <div className="sm:col-span-2">
          <LocationLabel label="Chọn địa chỉ hành chính mới tương ứng">
            <select
              required
              value={selectedMappingKey}
              onChange={(event) => {
                const mapping = migrationOptions.find(
                  (item) =>
                    `${item.newProvinceName}|${item.newWardCode}` ===
                    event.target.value,
                );
                if (mapping) void applyMapping(mapping, value);
              }}
              className="input-field"
            >
              <option value="">Chọn địa chỉ v3</option>
              {migrationOptions.map((mapping) => (
                <option
                  key={`${mapping.newProvinceName}-${mapping.newWardCode}`}
                  value={`${mapping.newProvinceName}|${mapping.newWardCode}`}
                >
                  {mapping.newWardName}, {mapping.newProvinceName}
                </option>
              ))}
            </select>
          </LocationLabel>
        </div>
      ) : null}

      {migrationError ? (
        <p className="sm:col-span-2 rounded-xl bg-rose-50 p-4 text-sm text-rose-700">
          {migrationError}
        </p>
      ) : null}

      {value.address && value.province && value.ward ? (
        <div className="sm:col-span-2 rounded-xl border border-green-200 bg-green-50 p-4 text-sm text-green-800">
          <strong className="block">Địa chỉ hành chính sẽ được lưu</strong>
          <span>
            {value.address}, {value.ward}, {value.province} · Mã {value.wardCode}/{value.provinceCode}
          </span>
        </div>
      ) : null}
    </div>
  );
}

function LocationLabel({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-bold text-slate-700">
        {label}
      </span>
      {children}
    </label>
  );
}
