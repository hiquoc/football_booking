package com.project.booking.community.repository;

import com.project.booking.community.entity.CommunityPostReport;
import com.project.booking.community.enums.CommunityReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CommunityPostReportRepository extends JpaRepository<CommunityPostReport, UUID> {
    @Query("""
            SELECT COUNT(r) > 0
            FROM CommunityPostReport r
            WHERE r.post.id = :postId
              AND r.reporterId = :reporterId
            """)
    boolean existsByPostIdAndReporterId(@Param("postId") UUID postId, @Param("reporterId") UUID reporterId);

    @Query("SELECT r FROM CommunityPostReport r WHERE (:status IS NULL OR r.status = :status)")
    Page<CommunityPostReport> findForReview(@Param("status") CommunityReportStatus status, Pageable pageable);
}
