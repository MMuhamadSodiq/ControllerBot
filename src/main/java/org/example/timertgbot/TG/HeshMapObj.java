package org.example.timertgbot.TG;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HeshMapObj {
    private Date date;
    private Long chatId;
    private UUID creditId;
    private String lastUpdatedDate;
}
