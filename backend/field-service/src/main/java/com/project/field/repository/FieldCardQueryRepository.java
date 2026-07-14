package com.project.field.repository;

import com.project.common.exception.BadRequestException;
import com.project.field.dto.FieldCardDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FieldCardQueryRepository {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "rating", "f.rating_average",
            "reviews", "f.total_reviews",
            "newest", "f.created_at",
            "distance", "distance_km");

    private static final String DISTANCE_SQL = """
            6371 * acos(least(1, greatest(-1,
                cos(radians(:latitude)) * cos(radians(f.latitude))
                * cos(radians(f.longitude) - radians(:longitude))
                + sin(radians(:latitude)) * sin(radians(f.latitude))
            )))
            """;

    private final EntityManager entityManager;

    public Page<FieldCardDto> search(
            String keyword,
            String fieldType,
            String subFieldType,
            String district,
            String provinceCode,
            BigDecimal latitude,
            BigDecimal longitude,
            Double radiusKm,
            String sortBy,
            String direction,
            int page,
            int size,
            UUID userId) {
        boolean hasLocation = latitude != null && longitude != null;
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException("Latitude and longitude must be provided together");
        }
        if ((radiusKm != null || "distance".equalsIgnoreCase(sortBy)) && !hasLocation) {
            throw new BadRequestException("Location is required for distance filtering or sorting");
        }

        String normalizedSort = sortBy == null ? "rating" : sortBy.toLowerCase(Locale.ROOT);
        String sortColumn = SORT_COLUMNS.get(normalizedSort);
        if (sortColumn == null) {
            throw new BadRequestException("sortBy must be one of: rating, reviews, newest, distance");
        }
        String sortDirection = "asc".equalsIgnoreCase(direction) ? "ASC" : "DESC";
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        String distanceSelect = hasLocation ? DISTANCE_SQL : "NULL";
        StringBuilder where = new StringBuilder("""
                WHERE f.status = 'APPROVED' AND f.active = true AND f.deleted = false
                """);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND vietnamese_search_normalize(f.name) LIKE '%' || vietnamese_search_normalize(:keyword) || '%'");
        }
        if (fieldType != null && !fieldType.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM field_field_types fft JOIN field_types ft ON ft.id = fft.field_type_id WHERE fft.field_id = f.id AND ft.deleted = false AND ft.active = true AND ft.sport_type = :fieldType)");
        }
        if (subFieldType != null && !subFieldType.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM sub_fields sf WHERE sf.field_id = f.id AND sf.deleted = false AND sf.active = true AND sf.sub_field_type = :subFieldType)");
        }
        if (district != null && !district.isBlank()) {
            where.append(" AND vietnamese_search_normalize(f.legacy_district) = vietnamese_search_normalize(:district)");
        }
        if (provinceCode != null && !provinceCode.isBlank()) {
            where.append(" AND f.province_code = :provinceCode");
        }
        if (radiusKm != null) {
            where.append(" AND ").append(DISTANCE_SQL).append(" <= :radiusKm");
        }

        String favoriteSelect = userId == null
                ? "false"
                : "EXISTS (SELECT 1 FROM field_favorites ff WHERE ff.field_id = f.id AND ff.user_id = :userId AND ff.deleted = false)";

        String selectSql = """
                SELECT f.id, f.name, f.address, f.ward, f.province, f.latitude, f.longitude,
                       f.rating_average, f.total_reviews,
                       (SELECT fi.image_url FROM field_images fi WHERE fi.field_id = f.id
                        ORDER BY fi.is_primary DESC, fi.display_order ASC, fi.id ASC LIMIT 1) AS primary_image_url,
                       (SELECT string_agg(DISTINCT ft.sport_type, ',' ORDER BY ft.sport_type)
                        FROM field_field_types fft JOIN field_types ft ON ft.id = fft.field_type_id
                        WHERE fft.field_id = f.id AND ft.deleted = false AND ft.active = true) AS field_types,
                """ + distanceSelect + " AS distance_km, " + favoriteSelect + " AS is_favorite FROM fields f " + where
                + " ORDER BY " + sortColumn + " " + sortDirection + ", f.id ASC";
        String countSql = "SELECT count(*) FROM fields f " + where;

        Query dataQuery = entityManager.createNativeQuery(selectSql);
        Query countQuery = entityManager.createNativeQuery(countSql);
        bind(dataQuery, keyword, fieldType, subFieldType, district, provinceCode, latitude, longitude, radiusKm, hasLocation);
        bind(countQuery, keyword, fieldType, subFieldType, district, provinceCode, latitude, longitude, radiusKm, hasLocation && radiusKm != null);
        if (userId != null) dataQuery.setParameter("userId", userId);
        dataQuery.setFirstResult(safePage * safeSize);
        dataQuery.setMaxResults(safeSize);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<FieldCardDto> cards = rows.stream().map(this::mapRow).toList();
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new PageImpl<>(cards, PageRequest.of(safePage, safeSize), total);
    }

    private void bind(Query query, String keyword, String fieldType, String subFieldType, String district, String provinceCode,
            BigDecimal latitude, BigDecimal longitude, Double radiusKm, boolean bindLocation) {
        if (keyword != null && !keyword.isBlank()) query.setParameter("keyword", keyword);
        if (fieldType != null && !fieldType.isBlank()) query.setParameter("fieldType", fieldType);
        if (subFieldType != null && !subFieldType.isBlank()) query.setParameter("subFieldType", subFieldType);
        if (district != null && !district.isBlank()) query.setParameter("district", district);
        if (provinceCode != null && !provinceCode.isBlank()) query.setParameter("provinceCode", provinceCode);
        if (bindLocation) {
            query.setParameter("latitude", latitude);
            query.setParameter("longitude", longitude);
        }
        if (radiusKm != null) query.setParameter("radiusKm", radiusKm);
    }

    private FieldCardDto mapRow(Object[] row) {
        String types = (String) row[10];
        return FieldCardDto.builder()
                .id((UUID) row[0])
                .name((String) row[1])
                .address((String) row[2])
                .ward((String) row[3])
                .province((String) row[4])
                .latitude((BigDecimal) row[5])
                .longitude((BigDecimal) row[6])
                .ratingAverage((BigDecimal) row[7])
                .totalReviews(((Number) row[8]).intValue())
                .primaryImageUrl((String) row[9])
                .fieldTypes(types == null || types.isBlank() ? List.of() : Arrays.asList(types.split(",")))
                .distanceKm(row[11] == null ? null : ((Number) row[11]).doubleValue())
                .isFavorite((Boolean) row[12])
                .build();
    }
}
