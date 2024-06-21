package org.example.timertgbot.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.timertgbot.TG.CreditStatus;
import org.glassfish.grizzly.http.util.TimeStamp;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
public class Credit {
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String client_name;
    private Integer amount;
    @ManyToOne(fetch = FetchType.EAGER)
    @Cascade(CascadeType.REMOVE)
    private Staff staff;
    private Date endDate;
    private Integer messageId;
    private Integer compensation = 0;
    private Integer reward = 0;
    @Enumerated(EnumType.STRING)
    private CreditStatus creditStatus;

}
