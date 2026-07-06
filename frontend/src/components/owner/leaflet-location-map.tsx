"use client";

import { useMemo } from "react";
import L from "leaflet";
import {
  MapContainer,
  Marker,
  TileLayer,
  useMapEvents,
} from "react-leaflet";

const VIETNAM_CENTER: [number, number] = [16.047079, 108.20623];

export default function LeafletLocationMap({
  latitude,
  longitude,
  onChange,
}: {
  latitude: number | null;
  longitude: number | null;
  onChange: (latitude: number, longitude: number) => void;
}) {
  const position =
    latitude === null || longitude === null
      ? null
      : ([latitude, longitude] as [number, number]);
  const markerIcon = useMemo(
    () =>
      L.divIcon({
        className: "field-location-marker",
        html: '<span aria-hidden="true"></span>',
        iconSize: [32, 40],
        iconAnchor: [16, 40],
      }),
    [],
  );

  return (
    <MapContainer
      center={position ?? VIETNAM_CENTER}
      zoom={position ? 15 : 5}
      scrollWheelZoom
      className="h-full min-h-80 w-full"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <MapClickHandler onChange={onChange} />
      {position ? (
        <Marker
          draggable
          icon={markerIcon}
          position={position}
          eventHandlers={{
            dragend: (event) => {
              const point = event.target.getLatLng();
              onChange(point.lat, point.lng);
            },
          }}
        />
      ) : null}
    </MapContainer>
  );
}

function MapClickHandler({
  onChange,
}: {
  onChange: (latitude: number, longitude: number) => void;
}) {
  useMapEvents({
    click(event) {
      onChange(event.latlng.lat, event.latlng.lng);
    },
  });
  return null;
}
