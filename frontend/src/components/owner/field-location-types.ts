export interface FieldLocationValue {
  address: string;
  ward: string;
  wardCode: string;
  province: string;
  provinceCode: string;
  legacyWard: string;
  legacyWardCode: string;
  legacyDistrict: string;
  legacyProvince: string;
  latitude: number | null;
  longitude: number | null;
}

export const emptyFieldLocation: FieldLocationValue = {
  address: "",
  ward: "",
  wardCode: "",
  province: "",
  provinceCode: "",
  legacyWard: "",
  legacyWardCode: "",
  legacyDistrict: "",
  legacyProvince: "",
  latitude: null,
  longitude: null,
};
