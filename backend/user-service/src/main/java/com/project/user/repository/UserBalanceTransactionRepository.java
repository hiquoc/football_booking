package com.project.user.repository;

import com.project.user.entity.UserBalanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserBalanceTransactionRepository extends JpaRepository<UserBalanceTransaction, UUID> {
    boolean existsByOperationKey(String operationKey);
}
