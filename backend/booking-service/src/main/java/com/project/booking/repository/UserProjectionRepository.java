package com.project.booking.repository;

import com.project.booking.entity.UserProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserProjectionRepository extends JpaRepository<UserProjection, UUID> {
    @Query("""
            SELECT u.userId AS userId, u.fullName AS username, u.phoneNumber AS phoneNumber, u.status AS status
            FROM UserProjection u
            WHERE u.userId IN :userIds
            """)
    List<UserContactView> findContactByUserIdIn(@Param("userIds") Collection<UUID> userIds);

    interface UserContactView {
        UUID getUserId();
        String getUsername();
        String getPhoneNumber();
        String getStatus();
    }
}
