package com.showroom.inventory.repository;

import com.showroom.inventory.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByOrderByCreatedAtDesc();

    List<ActivityLog> findByUsernameIgnoreCaseOrderByCreatedAtDesc(String username);

    List<ActivityLog> findByModuleAffectedIgnoreCaseOrderByCreatedAtDesc(String moduleAffected);
}

