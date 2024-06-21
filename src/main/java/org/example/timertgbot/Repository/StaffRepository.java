package org.example.timertgbot.Repository;

import org.example.timertgbot.Entity.Staff;
import org.example.timertgbot.TG.Role;
import org.example.timertgbot.TG.StaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {
    void deleteByName(String name);
    Optional<Staff> findByPhoneAndStaffStatus(String phone,StaffStatus staffStatus);
    Optional<Staff> findByChatId(Long chatId);
    List<Staff> findAllByRoleAndStaffStatus(Role role, StaffStatus staffStatus);
    Optional<Staff> findByName(String name);
}
