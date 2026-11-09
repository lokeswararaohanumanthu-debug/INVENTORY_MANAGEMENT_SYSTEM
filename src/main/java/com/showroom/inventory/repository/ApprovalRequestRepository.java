package com.showroom.inventory.repository;

import com.showroom.inventory.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findAllByOrderByRequestedAtDesc();

    List<ApprovalRequest> findByStatusOrderByRequestedAtDesc(String status);

    List<ApprovalRequest> findByRequestedByUsernameIgnoreCaseOrderByRequestedAtDesc(String username);

    Optional<ApprovalRequest> findByRequestCode(String requestCode);

    long countByStatus(String status);
}

