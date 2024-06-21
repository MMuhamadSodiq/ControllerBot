package org.example.timertgbot.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.timertgbot.TG.Role;
import org.example.timertgbot.TG.STEP;
import org.example.timertgbot.TG.StaffStatus;

import java.rmi.server.UID;
import java.util.UUID;

@Entity
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class Staff {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private UUID id;
    private String name;
    @Column(unique = true)
    private String phone;
    @Column(unique = true)
    private Long chatId;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Enumerated(EnumType.STRING)
    private STEP step;
    @Enumerated(EnumType.STRING)
    private StaffStatus staffStatus;
}

