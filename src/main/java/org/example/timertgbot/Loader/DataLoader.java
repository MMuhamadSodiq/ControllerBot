package org.example.timertgbot.Loader;

import lombok.RequiredArgsConstructor;
import org.example.timertgbot.Entity.Staff;
import org.example.timertgbot.Repository.StaffRepository;
import org.example.timertgbot.TG.Role;
import org.example.timertgbot.TG.STEP;
import org.example.timertgbot.TG.StaffStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final StaffRepository staffRepository;

    @Override
    public void run(String... args) throws Exception {
        if (staffRepository.findAllByRoleAndStaffStatus(Role.HEADMASTER, StaffStatus.ACTIVE).isEmpty()) {
            staffRepository.save(Staff.builder()
                    .name("Mansurbek")
                    .phone("998907100805")
                    .role(Role.HEADMASTER)
                    .chatId(null)
                    .step(STEP.START)
                            .staffStatus(StaffStatus.ACTIVE)
                    .build());
        }
    }
}
