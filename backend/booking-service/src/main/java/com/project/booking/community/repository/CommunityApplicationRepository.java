package com.project.booking.community.repository;

import com.project.booking.community.entity.CommunityApplication;
import com.project.booking.community.enums.CommunityApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityApplicationRepository extends JpaRepository<CommunityApplication, UUID> {
    @Query("""
            SELECT COUNT(a) > 0
            FROM CommunityApplication a
            WHERE a.post.id = :postId
              AND a.applicantId = :applicantId
              AND a.status IN :statuses
            """)
    boolean existsActiveApplication(
            @Param("postId") UUID postId,
            @Param("applicantId") UUID applicantId,
            @Param("statuses") Collection<CommunityApplicationStatus> statuses);

    @Query("SELECT a FROM CommunityApplication a WHERE a.id = :id AND a.post.id = :postId")
    Optional<CommunityApplication> findByIdAndPostId(@Param("id") UUID id, @Param("postId") UUID postId);

    @Query("""
            SELECT a
            FROM CommunityApplication a
            WHERE a.post.id = :postId
              AND a.applicantId = :applicantId
              AND a.status = :status
            """)
    Optional<CommunityApplication> findByPostIdAndApplicantIdAndStatus(
            @Param("postId") UUID postId,
            @Param("applicantId") UUID applicantId,
            @Param("status") CommunityApplicationStatus status);

    @Query("""
            SELECT a
            FROM CommunityApplication a
            WHERE a.post.id = :postId
              AND a.status = :status
            """)
    List<CommunityApplication> findByPostIdAndStatus(
            @Param("postId") UUID postId,
            @Param("status") CommunityApplicationStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CommunityApplication a
            SET a.status = :rejectedStatus,
                a.decidedAt = CURRENT_TIMESTAMP
            WHERE a.post.id = :postId
              AND a.id <> :acceptedApplicationId
              AND a.status = :pendingStatus
            """)
    int rejectOtherPendingApplications(
            @Param("postId") UUID postId,
            @Param("acceptedApplicationId") UUID acceptedApplicationId,
            @Param("pendingStatus") CommunityApplicationStatus pendingStatus,
            @Param("rejectedStatus") CommunityApplicationStatus rejectedStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CommunityApplication a
            SET a.status = :rejectedStatus,
                a.decidedAt = CURRENT_TIMESTAMP
            WHERE a.post.id = :postId
              AND a.status = :pendingStatus
            """)
    int rejectPendingApplicationsForPost(
            @Param("postId") UUID postId,
            @Param("pendingStatus") CommunityApplicationStatus pendingStatus,
            @Param("rejectedStatus") CommunityApplicationStatus rejectedStatus);
}
