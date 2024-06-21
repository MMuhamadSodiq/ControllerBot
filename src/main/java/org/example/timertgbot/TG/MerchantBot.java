package org.example.timertgbot.TG;

import lombok.SneakyThrows;
import org.example.timertgbot.Entity.Credit;
import org.example.timertgbot.Entity.Staff;
import org.example.timertgbot.Repository.CreditRepository;
import org.example.timertgbot.Repository.StaffRepository;
import org.example.timertgbot.TimertgbotApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MerchantBot extends TelegramLongPollingBot {
    private final TimertgbotApplication timertgbotApplication;
    private Credit newCredit = new Credit();
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
    private final String botToken = "7485074434:AAH2U9yO6c0qMrlwQHrSGtQ9uB5gjBl4vQE";
    private final String username = "@CantrollerBot";
    private final StaffRepository staffRepository;
    private final CreditRepository creditRepository;
    private Staff currentStaff = null;
    private Credit addingCredit = null;
    private Date todaysDate = new Date();
    private HashMap<Integer, HeshMapObj> messageIds = new HashMap<>();
    LocalDateTime now = LocalDateTime.now();
    LocalTime eightAM = LocalTime.of(8, 0);
    LocalTime midnight = LocalTime.of(0, 0);


    @SneakyThrows
    @Autowired
    public MerchantBot(TelegramBotsApi api, StaffRepository staffRepository, CreditRepository creditRepository, TimertgbotApplication timertgbotApplication) throws TelegramApiException {
        this.staffRepository = staffRepository;
        this.creditRepository = creditRepository;
        api.registerBot(this);
        this.timertgbotApplication = timertgbotApplication;
    }


    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                currentStaff = registeredUser(message);
                if (currentStaff != null) {
                    if (message.getText().equals("/start")) {
                        if (currentStaff.getRole().equals(Role.HEADMASTER)) {
                            updateSTEP(STEP.START);
                        } else {
                            updateSTEP(STEP.STAFF_START);
                        }
                    }
                    if (currentStaff.getRole().equals(Role.HEADMASTER)) {
                        headmaster(message);
                    } else {
                        if (currentStaff.getStep().equals(STEP.STAFF_START)) {
                            if (message.getText().equals("/start")) {
                                starting(message);
                            } else if (message.getText().equals("Кредиты")) {
                                List<Credit> credits = creditRepository.findAllByStaff(currentStaff.getId());
                                String filePath = "credits.xlsx";
                                try {
                                    // Generate Excel file
                                    ExcelGenerator.generateExcelStaff(credits, filePath);

                                    // Send file
                                    SendDocument sendDocument = new SendDocument();
                                    sendDocument.setChatId(currentStaff.getChatId());
                                    sendDocument.setDocument(new InputFile(new java.io.File(filePath)));

                                    execute(sendDocument);

                                } catch (IOException | TelegramApiException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(message.getChatId().toString());
                            sendMessage.setText("Вы вошли в систему как сотрудник! \uD83E\uDDD1\u200D\uD83D\uDCBC \n" +
                                    "Оставайтесь с нами, чтобы получить больше миссий! \uD83D\uDCC3");

                            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                            replyKeyboardMarkup.setResizeKeyboard(true);
                            replyKeyboardMarkup.setSelective(true);
                            replyKeyboardMarkup.setOneTimeKeyboard(true);

                            List<KeyboardRow> keyboardRows = new ArrayList<>();

                            KeyboardRow keyboardRow = new KeyboardRow();

                            KeyboardButton keyboardButton = new KeyboardButton();

                            keyboardButton.setText("Кредиты");

                            keyboardRow.add(keyboardButton);

                            keyboardRows.add(keyboardRow);

                            replyKeyboardMarkup.setKeyboard(keyboardRows);

                            sendMessage.setReplyMarkup(replyKeyboardMarkup);

                            execute(sendMessage);
                            updateSTEP(STEP.STAFF_START);
                        }
                    }
                } else if (message.getText().equals("/start")) {
                    starting(message);
                }
            } else if (message.hasContact()) {
                if (checkIfForwarded(message)) {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setText("Не пытайтесь обмануть нас !❌ \n" +
                            "Отправьте свой контакт,не чужой! ‼\uFE0F");
                    sendMessage.setChatId(message.getChatId().toString());
                    execute(sendMessage);
                } else {
                    Contact contact = message.getContact();
                    String phoneNumber = contact.getPhoneNumber().substring(contact.getPhoneNumber().lastIndexOf('+') + 1);
                    Staff staff = isStaff(phoneNumber, message.getChatId());
                    if (staff != null) {

                        currentStaff = staff;
                        if (staff.getRole().equals(Role.HEADMASTER)) {
                            staff.setChatId(message.getChatId());
                            start(message);
                            updateSTEP(STEP.START);
                        } else {
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(message.getChatId().toString());
                            sendMessage.setText("Вы вошли в систему как сотрудник! \uD83E\uDDD1\u200D\uD83D\uDCBC \n" +
                                    "Оставайтесь с нами, чтобы получить больше миссий! \uD83D\uDCC3");

                            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                            replyKeyboardMarkup.setResizeKeyboard(true);
                            replyKeyboardMarkup.setSelective(true);
                            replyKeyboardMarkup.setOneTimeKeyboard(true);

                            List<KeyboardRow> keyboardRows = new ArrayList<>();

                            KeyboardRow keyboardRow = new KeyboardRow();

                            KeyboardButton keyboardButton = new KeyboardButton();

                            keyboardButton.setText("Кредиты");

                            keyboardRow.add(keyboardButton);

                            keyboardRows.add(keyboardRow);

                            replyKeyboardMarkup.setKeyboard(keyboardRows);

                            sendMessage.setReplyMarkup(replyKeyboardMarkup);

                            execute(sendMessage);

                            sendCredits(staff, message.getChatId());
                            staff.setChatId(message.getChatId());
                            updateSTEP(STEP.STAFF_START);
                        }
                    }
                }

            }
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (data.startsWith("REWARD")) {
                UUID id = UUID.fromString(data.substring(data.lastIndexOf("=") + 1));

                addingCredit = creditRepository.findById(id).get();

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("Введите Сумму:");
                execute(sendMessage);
                updateSTEP(STEP.ADDING_REWARD);
            } else if (data.startsWith("COMPENSATION")) {
                UUID id = UUID.fromString(data.substring(data.lastIndexOf("=") + 1));
                addingCredit = creditRepository.findById(id).get();
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("Введите Сумму:");
                execute(sendMessage);
                updateSTEP(STEP.ADDING_COMPENSATION);
            } else if (data.startsWith("NO_CONFIRM_FINISH_CREDIT")) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId);
                deleteMessage.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                execute(deleteMessage);
            } else if (data.startsWith("FINISHED_CREDIT_CONFIRM")) {

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("‼\uFE0F Вы точно закончили кредит? ‼\uFE0F");

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                List<InlineKeyboardButton> buttons = new ArrayList<>();

                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setCallbackData("FINISHED_CREDIT id=" + data.substring(data.indexOf("=") + 1) + " msg%" + update.getCallbackQuery().getMessage().getMessageId());
                inlineKeyboardButton.setText("Да ✅");
                InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
                inlineKeyboardButton1.setText("Ещё нет \uD83D\uDED1");
                inlineKeyboardButton1.setCallbackData("NO_CONFIRM_FINISH_CREDIT");
                buttons.add(inlineKeyboardButton);
                buttons.add(inlineKeyboardButton1);

                inlineKeyboardMarkup.setKeyboard(List.of(buttons));

                sendMessage.setReplyMarkup(inlineKeyboardMarkup);

                execute(sendMessage);
            } else if (data.startsWith("FINISHED_CREDIT")) {

                String idString = data.substring(data.lastIndexOf("=") + 1, data.lastIndexOf("msg") - 1);
                UUID id = UUID.fromString(idString);
                Integer msgId = Integer.parseInt(data.substring(data.lastIndexOf("%") + 1));

                Credit byId = creditRepository.findById(id).get();
                Staff personal = byId.getStaff();

                List<Staff> allByRole = staffRepository.findAllByRoleAndStaffStatus(Role.HEADMASTER,StaffStatus.ACTIVE);
                Staff headmaster = allByRole.get(0);

                DecimalFormat formatter = new DecimalFormat("#,###");
                String formattedAmount = formatter.format(byId.getAmount()).replace(',', '.');

                // Date formatter for parsing
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                // Convert to LocalDateTime
                LocalDateTime parsedDate = LocalDateTime.parse(byId.getEndDate().toString(), inputFormatter);

                // Convert java.util.Date to ZonedDateTime
                ZonedDateTime nowDate = ZonedDateTime.now();

                String formattedEndDate = parsedDate.format(outputFormatter);
                String formattedNowDate = nowDate.format(outputFormatter);

                String text = "‼\uFE0F  ЗАВЕРШЕННЫЙ КРЕДИТ   ‼\uFE0F\n" +
                        "\n" +
                        "\uD83D\uDCB5 Информация о кредите:\n" +
                        "\n" +
                        "\uD83D\uDD35 Имя клиента - " + byId.getClient_name() + "\n" +
                        "\uD83D\uDCB0 Сумма - " + formattedAmount + "\n" +
                        "\uD83D\uDCC5 Дата окончания - " + formattedEndDate + "\n" +
                        "\uD83D\uDCC5 Сегодняшняя дата - " + formattedNowDate + "\n" +
                        "\n" +
                        "\uD83E\uDDD1\u200D\uD83D\uDCBC Ответственный персонал - " + personal.getName();

                SendMessage sendMessage = new SendMessage();
                sendMessage.setText(text);
                sendMessage.setChatId(headmaster.getChatId());

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                List<InlineKeyboardButton> buttons = new ArrayList<>();

                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setCallbackData("REWARD id=" + byId.getId());
                inlineKeyboardButton.setText("Возноградить \uD83D\uDCB8");

                InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
                inlineKeyboardButton1.setCallbackData("COMPENSATION id=" + byId.getId());
                inlineKeyboardButton1.setText("Компенсация \uD83D\uDED1");

                buttons.add(inlineKeyboardButton);
                buttons.add(inlineKeyboardButton1);

                inlineKeyboardMarkup.setKeyboard(List.of(buttons));
                sendMessage.setReplyMarkup(inlineKeyboardMarkup);

                execute(sendMessage);
                byId.setCreditStatus(CreditStatus.COMPLETED);

                Credit save = creditRepository.save(byId);


                SendMessage sendMessage1 = new SendMessage();
                sendMessage1.setChatId(chatId);
                sendMessage1.setText("Поздравляем, вы выполнили этот кредит! ✅");
                execute(sendMessage1);

                DeleteMessage deleteMessage1 = new DeleteMessage();
                deleteMessage1.setChatId(chatId);
                deleteMessage1.setMessageId(msgId);
                execute(deleteMessage1);

                DeleteMessage deleteMessage2 = new DeleteMessage();
                deleteMessage2.setChatId(chatId);
                deleteMessage2.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                execute(deleteMessage2);


                messageIds.remove(save.getMessageId());
            } else if (currentStaff.getStep().equals(STEP.CONFIRMING_ADDING)) {
                if (data.equals("YES_CONFIRM_INFO_ADD")) {

                    newCredit.setCreditStatus(CreditStatus.ACTIVE);

                    Credit save = creditRepository.save(newCredit);


                    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");
                    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    ZonedDateTime parsedDate = ZonedDateTime.parse(newCredit.getEndDate().toString(), inputFormatter);

                    // Format the parsed date to the desired format
                    String formattedDate = parsedDate.format(outputFormatter);

                    Optional<Staff> byId = staffRepository.findById(newCredit.getStaff().getId());
                    if (byId.isPresent()) {
                        if (byId.get().getChatId() != null) {

                            Staff staffResponsible = byId.get();
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(staffResponsible.getChatId());
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            String formatted = formatter.format(newCredit.getAmount()).replace(',', '.');


                            LocalDateTime now = LocalDateTime.now();
                            LocalDateTime endDate = newCredit.getEndDate().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                            Duration duration = Duration.between(now, endDate);

                            long days = duration.toDays();
                            duration = duration.minus(days, ChronoUnit.DAYS);
                            long hours = duration.toHours();
                            duration = duration.minus(hours, ChronoUnit.HOURS);
                            long minutes = duration.toMinutes();


                            String timeRemaining = String.format("%d день, %d час, %d минут  ⏰", days, hours, minutes);


                            sendMessage.setText("У вас новый кредит!" +
                                    "\n" +
                                    "Кредитная информация: \n" +
                                    "\uD83D\uDD35 Имя клиента  - " + newCredit.getClient_name() + "\n" +
                                    "\uD83D\uDCB0 Сумма - " + formatted + " Сум \n" +
                                    "\uD83D\uDCC5 Дата окончания - " + formattedDate + "\n" +
                                    "\n" +
                                    "");

                            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                            List<InlineKeyboardButton> buttons = new ArrayList<>();
                            List<InlineKeyboardButton> buttons2 = new ArrayList<>();

                            buttons.add(InlineKeyboardButton.builder()
                                    .text("Завершено! ✅")
                                    .callbackData("FINISHED_CREDIT_CONFIRM id=" + save.getId())
                                    .build());

                            buttons2.add(InlineKeyboardButton.builder()
                                    .callbackData("A")
                                    .text(timeRemaining)
                                    .build());


                            inlineKeyboardMarkup.setKeyboard(List.of(buttons, buttons2));

                            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

                            Message execute = execute(sendMessage);
                            save.setMessageId(execute.getMessageId());
                            creditRepository.save(save);
                            messageIds.put(execute.getMessageId(), new HeshMapObj(newCredit.getEndDate(), save.getStaff().getChatId(), save.getId(), timeRemaining));
                        }
                    }

                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(currentStaff.getChatId());
                    sendMessage.setText("Кредит добавлен! ✅");
                    execute(sendMessage);
                    updateSTEP(STEP.START);
                    start(update.getCallbackQuery().getMessage());

                    newCredit = new Credit();

                } else if (data.equals("NO_CONFIRM_INFO_ADD")) {
                    Integer sizeOfStaffs = showListOfStaffs(update.getCallbackQuery().getMessage());
                    updateSTEP(STEP.SEARCHING_STAFF);
                }
            } else if (currentStaff.getStep().equals(STEP.CHECK_CONFIRMING)) {
                if (data.startsWith("YES_CONFIRM_INFO")) {
                    Integer sizeOfStaffs = showListOfStaffs(update.getCallbackQuery().getMessage());
                    if (sizeOfStaffs == 0) {
                        updateSTEP(STEP.START);
                    } else {
                        updateSTEP(STEP.SEARCHING_STAFF);
                    }
                } else if (data.startsWith("NO_CONFIRM_INFO")) {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Отправить информацию в формате \uD83D\uDCC4: \n " +
                            "1.Имя клиента \uD83D\uDC64\n" +
                            "2.Сумма \uD83D\uDCB5\n" +
                            "3.Дата окончания (01.01.2024) \uD83D\uDCC5\n" +
                            "\n" +
                            "После того, как вся информация будет заполнена, вам следует подобрать для нее персонал! \uD83E\uDDD1\u200D\uD83D\uDCBC");
                    execute(sendMessage);
                    updateSTEP(STEP.ADDING_CREDIT);
                }

            }
        }
        interval();
    }

    private void sendCredits(Staff staff, Long chatId) {


        // Format the parsed date to the desired format


        if (staff.getChatId() == null) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            List<Credit> allByCreditStatusAndStaffId = creditRepository.findAllByCreditStatusAndStaffId(CreditStatus.ACTIVE, staff.getId());

            DecimalFormat formatter = new DecimalFormat("#,###");


            LocalDateTime now = LocalDateTime.now();


            allByCreditStatusAndStaffId.forEach(credit -> {

                String formatted = formatter.format(credit.getAmount()).replace(',', '.');

                ZonedDateTime zonedDateTime = credit.getEndDate().toInstant().atZone(ZoneId.systemDefault());
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String formattedDate = zonedDateTime.format(inputFormatter);


                LocalDateTime endDate = credit.getEndDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                Duration duration = Duration.between(now, endDate);

                long days = duration.toDays();
                duration = duration.minus(days, ChronoUnit.DAYS);
                long hours = duration.toHours();
                duration = duration.minus(hours, ChronoUnit.HOURS);
                long minutes = duration.toMinutes();

                String timeRemaining = String.format("%d день, %d час, %d минут  ⏰", days, hours, minutes);


                sendMessage.setText("У вас новый кредит!" +
                        "\n" +
                        "Кредитная информация: \n" +
                        "\uD83D\uDD35 Имя клиента  - " + credit.getClient_name() + "\n" +
                        "\uD83D\uDCB0 Сумма - " + formatted + " Сум \n" +
                        "\uD83D\uDCC5 Дата окончания - " + formattedDate + "\n" +
                        "\n" +
                        "");

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                List<InlineKeyboardButton> buttons = new ArrayList<>();
                List<InlineKeyboardButton> buttons2 = new ArrayList<>();

                buttons.add(InlineKeyboardButton.builder()
                        .text("Завершено! ✅")
                        .callbackData("FINISHED_CREDIT_CONFIRM id=" + credit.getId())
                        .build());

                buttons2.add(InlineKeyboardButton.builder()
                        .callbackData("A")
                        .text(timeRemaining)
                        .build());


                inlineKeyboardMarkup.setKeyboard(List.of(buttons, buttons2));

                sendMessage.setReplyMarkup(inlineKeyboardMarkup);

                Message execute = null;
                try {
                    execute = execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                messageIds.put(execute.getMessageId(), new HeshMapObj(credit.getEndDate(), execute.getChatId(), newCredit.getId(), timeRemaining));
            });

        }
    }

    public Date getYesterdaysDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(todaysDate);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTime();
    }


    @Scheduled(fixedRate = 15000)
    public void interval() throws TelegramApiException {

        if (now.toLocalTime().equals(midnight)) {
            List<Credit> credits = creditRepository.findExpiredCredit(LocalDateTime.now());
            credits.forEach(credit -> {
                credit.setCreditStatus(CreditStatus.EXPIRED);
            });
            List<Credit> expiredCredits = creditRepository.saveAll(credits);
            DeleteMessage deleteMessage = new DeleteMessage();
            for (Credit expiredCredit : expiredCredits) {
                deleteMessage.setChatId(expiredCredit.getStaff().getChatId());
                deleteMessage.setMessageId(expiredCredit.getMessageId());
                execute(deleteMessage);
            }
        }

        if (now.toLocalTime().equals(eightAM)) {
            sendExpiredCredits();
        }

        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        messageIds.forEach((msgId, date) -> {
            editMessageReplyMarkup.setMessageId(msgId);
            editMessageReplyMarkup.setChatId(date.getChatId());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDate = date.getDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            Duration duration = Duration.between(now, endDate);

            long days = duration.toDays();
            duration = duration.minus(days, ChronoUnit.DAYS);
            long hours = duration.toHours();
            duration = duration.minus(hours, ChronoUnit.HOURS);
            long minutes = duration.toMinutes();
            String timeRemaining = String.format("%d день, %d час, %d минут  ⏰", days, hours, minutes);
            if (date.getLastUpdatedDate().equals(timeRemaining)) {
                return;
            } else {


                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                List<InlineKeyboardButton> buttons = new ArrayList<>();
                List<InlineKeyboardButton> buttons2 = new ArrayList<>();
                buttons.add(InlineKeyboardButton.builder()
                        .text("Завершено! ✅")
                        .callbackData("FINISHED_CREDIT_CONFIRM id=" + date.getCreditId())
                        .build());
                buttons2.add(InlineKeyboardButton.builder()
                        .callbackData("A")
                        .text(timeRemaining)
                        .build());

                inlineKeyboardMarkup.setKeyboard(List.of(buttons, buttons2));

                editMessageReplyMarkup.setReplyMarkup(inlineKeyboardMarkup);
                try {
                    execute(editMessageReplyMarkup);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                date.setLastUpdatedDate(timeRemaining);
            }
        });


        LocalDateTime localDateTime = todaysDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        LocalDateTime localDateTime1 = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // Extract day of the month
        int dayOfMonth = localDateTime.getDayOfMonth();
        int dayOfMonth1 = localDateTime1.getDayOfMonth();
        if (dayOfMonth1 != dayOfMonth) {
            sendDailyMessagesToStaff();
        }
        todaysDate = new Date(); // Update the date to today's date

    }

    @SneakyThrows
    private void sendExpiredCredits() {
        List<Staff> allByRole = staffRepository.findAllByRoleAndStaffStatus(Role.HEADMASTER,StaffStatus.ACTIVE);
        for (Staff head : allByRole) {

            if (head.getChatId() != null) {
                List<Credit> allByCreditStatus = creditRepository.findAllByCreditStatus(CreditStatus.ACTIVE);


                Date date = new Date();
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(head.getChatId());

                for (Credit credit : allByCreditStatus) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd");
                    Integer dayOfMonth = Integer.parseInt(sdf.format(date));
                    Integer dayOfMonth1 = Integer.parseInt(sdf.format(credit.getEndDate()));
                    if (dayOfMonth > dayOfMonth1) {
                        DecimalFormat formatter = new DecimalFormat("#,###");
                        String formattedAmount = formatter.format(credit.getAmount()).replace(',', '.');
                        String text = String.format("\uFE0F ПРОРОЧЕННЫЙ КРЕДИТ   ‼\uFE0F\n" +
                                        "\uD83D\uDCB5 Информация о кредите:\n" +
                                        "\uD83D\uDD35 Клиент: %s\n" +
                                        "\uD83D\uDCB0 Сумма: %s\n" +
                                        "👤 Персонал в ответе - " + credit.getStaff().getName() + "\n" +
                                        "\uD83D\uDCC5 Дата окончания : %s\n" +
                                        "\uD83D\uDCC5 Сегодняшняя дата: %s",
                                credit.getClient_name(),
                                formattedAmount,
                                formatDate(credit.getEndDate()),
                                formatDate(new Date()));
                        sendMessage.setText(text);
                        execute(sendMessage);
                    }
                }
            }

        }
    }

    @SneakyThrows
    private void sendDailyMessagesToStaff() {
        List<Credit> allByCreditStatus = creditRepository.findAllByCreditStatusOrderByEndDate(CreditStatus.ACTIVE);
        SendMessage sendMessage = new SendMessage();
        DecimalFormat formatter = new DecimalFormat("#,###");
        for (Credit credit : allByCreditStatus) {
            if (credit.getStaff().getChatId() != null) {

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime endDate = credit.getEndDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                Duration duration = Duration.between(now, endDate);
                long days = duration.toDays();
                duration = duration.minus(days, ChronoUnit.DAYS);
                long hours = duration.toHours();
                duration = duration.minus(hours, ChronoUnit.HOURS);
                long minutes = duration.toMinutes();
                String timeRemaining = String.format("%d день, %d час, %d минут  ⏰", days, hours, minutes);

                String formattedAmount = formatter.format(credit.getAmount()).replace(',', '.');
                String text = String.format("\uFE0F У ВАС  НЕЗАВЕРШЕННЫЙ КРЕДИТ   ‼\uFE0F\n" +
                                "\uD83D\uDCB5 Информация о кредите:\n" +
                                "\uD83D\uDD35 Клиент: %s\n" +
                                "\uD83D\uDCB0 Сумма: %s\n" +
                                "\uD83D\uDCC5 Дата окончания : %s\n" +
                                "\uD83D\uDCC5 Сегодняшняя дата: %s",
                        credit.getClient_name(),
                        formattedAmount,
                        formatDate(credit.getEndDate()),
                        formatDate(new Date()));

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                List<InlineKeyboardButton> buttons = new ArrayList<>();
                List<InlineKeyboardButton> buttons2 = new ArrayList<>();
                buttons.add(InlineKeyboardButton.builder()
                        .text("Завершено! ✅")
                        .callbackData("FINISHED_CREDIT_CONFIRM id=" + credit.getId())
                        .build());
                buttons2.add(InlineKeyboardButton.builder()
                        .callbackData("A")
                        .text(timeRemaining)
                        .build());

                inlineKeyboardMarkup.setKeyboard(List.of(buttons, buttons2));

                sendMessage.setReplyMarkup(inlineKeyboardMarkup);

                sendMessage.setChatId(credit.getStaff().getChatId());
                sendMessage.setText(text);
                execute(sendMessage);
            }
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        return formatter.format(date);
    }

    public void sendCreditsExcel(long chatId) {


        List<Credit> credits = creditRepository.findAllByLastMonth();
        String filePath = "credits.xlsx";
        try {
            // Generate Excel file
            ExcelGenerator.generateExcel(credits, filePath);

            // Send file
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(chatId);
            sendDocument.setDocument(new InputFile(new java.io.File(filePath)));

            execute(sendDocument);

        } catch (IOException | TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    private Integer showListOfStaffs(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        List<Staff> all = staffRepository.findAllByRoleAndStaffStatus(Role.STAFF,StaffStatus.ACTIVE);

        int counter = 0;
        List<KeyboardRow> rows = new ArrayList<>();

        for (int i = 0; i < all.size(); i++) {
            KeyboardRow perRow = new KeyboardRow();
            perRow.add(KeyboardButton.builder()
                    .text(all.get(i).getName() + " \uD83E\uDDD1\u200D\uD83D\uDCBC")
                    .build());
            if (counter <= 3) {
                counter = 0;
                rows.add(perRow);
            }
            ;
            counter++;
        }

        String text = all.isEmpty() ? "Персонала пока нет!" : "Выберите персонала для кредита:";


        replyKeyboardMarkup.setKeyboard(rows);

        sendMessage.setText(text);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        execute(sendMessage);
        return all.size();
    }

    private boolean checkIfForwarded(Message message) {
        return !message.getFrom().getId().equals(message.getContact().getUserId());
    }

    private Staff registeredUser(Message message) {
        Optional<Staff> byChatId = staffRepository.findByChatId(message.getChatId());


        return byChatId.orElse(null);
    }

    @SneakyThrows
    private void headmaster(Message message) {
        if (currentStaff.getStep().equals(STEP.ADDING_REWARD)) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            int amount = 0;
            try {
                amount = Integer.parseInt(message.getText());
                addingCredit.setReward(addingCredit.getReward() + amount);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId());
                String formatted = formatter.format(addingCredit.getReward()).replace(',', '.');
                sendMessage.setText("Вознаграждение в сумме " + formatted + " сум добавлена к кредиту! ✅");
                execute(sendMessage);
                creditRepository.save(addingCredit);
                updateSTEP(STEP.START);
            } catch (NumberFormatException e) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId());
                sendMessage.setText("Введите цифру!");
                execute(sendMessage);
            }
        } else if (currentStaff.getStep().equals(STEP.ADDING_COMPENSATION)) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            int amount = 0;
            try {
                amount = Integer.parseInt(message.getText());
                addingCredit.setCompensation(addingCredit.getCompensation() + amount);
                String formatted = formatter.format(addingCredit.getCompensation()).replace(',', '.');
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId());
                sendMessage.setText("Компенсация в сумме " + formatted + " сум добавлена к кредиту! ✅");
                execute(sendMessage);
                creditRepository.save(addingCredit);
                updateSTEP(STEP.START);
            } catch (NumberFormatException e) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId());
                sendMessage.setText("Введите цифру!");
                execute(sendMessage);
            }
        } else if (currentStaff.getStep().equals(STEP.SEARCHING_STAFF)) {
            String nameOf = message.getText().substring(0, message.getText().lastIndexOf(" "));
            Optional<Staff> byName = staffRepository.findByName(nameOf);
            if (byName.isPresent()) {
                Staff staffFound = byName.get();
                newCredit.setStaff(staffFound);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId());
                DecimalFormat formatter = new DecimalFormat("#,###");
                String formatted = formatter.format(newCredit.getAmount()).replace(',', '.');

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                List<InlineKeyboardButton> buttons = new ArrayList<>();

                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();

                inlineKeyboardButton.setText("Да ✅");
                inlineKeyboardButton.setCallbackData("YES_CONFIRM_INFO_ADD");

                InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();


                inlineKeyboardButton1.setText("Нет ❌");
                inlineKeyboardButton1.setCallbackData("NO_CONFIRM_INFO_ADD");
                buttons.add(inlineKeyboardButton);
                buttons.add(inlineKeyboardButton1);

                inlineKeyboardMarkup.setKeyboard(List.of(buttons));

                sendMessage.setReplyMarkup(inlineKeyboardMarkup);

                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");


                ZonedDateTime parsedDate = ZonedDateTime.parse(newCredit.getEndDate().toString(), inputFormatter);
                String formattedDate = parsedDate.format(outputFormatter);


                sendMessage.setText("Информация о кредите: \n" +
                        "\n" +
                        "🔵 Имя клиента - " + newCredit.getClient_name() + "\n" + // Blue dot emoji
                        "💰 Сумма - " + formatted + " Сум \n " + // Money bag emoji
                        "📅 Дата окончания - <b>" + formattedDate + "</b> \n" + // Calendar emoji
                        "👤 Персонал в ответе - " + newCredit.getStaff().getName() + "\n" +
                        "\n" +
                        " Завершить создания кредита?");

                sendMessage.enableHtml(true);

                execute(sendMessage);

                updateSTEP(STEP.CONFIRMING_ADDING);

            } else {
                SendMessage sendMessage = new SendMessage();
                ;
                sendMessage.setChatId(message.getChatId().toString());
                sendMessage.setText("Нет сотрудников с таким именем! Попробуйте еще раз!");
                execute(sendMessage);
                updateSTEP(STEP.START);
            }
        } else if (message.getText().startsWith("Сотрудники")) {
            List<Staff> all = staffRepository.findAllByRoleAndStaffStatus(Role.STAFF,StaffStatus.ACTIVE);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId());
            if (all.isEmpty()) {
                sendMessage.setText("Персоналов пока нет!");
                execute(sendMessage);
            } else {
                for (Staff staff : all) {
                    sendMessage.setText(staff.getName() + " " + (staff.getChatId() != null ? "\uD83E\uDD16" : "❌"));
                    execute(sendMessage);
                }
            }
        } else if(currentStaff.getStep().equals(STEP.REMOVE_STAFF_NAME)) {
            String nameOf = message.getText().substring(0, message.getText().lastIndexOf(" "));
            Staff staff = staffRepository.findByName(nameOf).get();
            staff.setStaffStatus(StaffStatus.ARCHIVE);
            staffRepository.save(staff);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText("Персонал был удален!");
            sendMessage.setChatId(currentStaff.getChatId());
            execute(sendMessage);
        } else if (message.getText().startsWith("Удаление персонала")) {
            showListOfStaffs(message);
            updateSTEP(STEP.REMOVE_STAFF_NAME);
        }
            else if (message.getText().startsWith("Добавить сотрудника!")) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText("Пожалуйста, отправьте информацию о сотрудниках в следующем формате : \uD83D\uDCC4 \n" +
                    "1.Имя \uD83D\uDC64 \n" +
                    "2.Номер телефона \uD83D\uDCDE");

            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setSelective(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);

            KeyboardRow keyboardRow = new KeyboardRow();
            KeyboardButton newButton = new KeyboardButton();
            newButton.setText("⬅\uFE0F Назад");
            keyboardRow.add(newButton);


            replyKeyboardMarkup.setKeyboard(List.of(keyboardRow));


            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            execute(sendMessage);
            updateSTEP(STEP.ADDING_STAFF);
        } else if (message.getText().startsWith("Cтатистика")) {
            sendCreditsExcel(message.getChatId());
        } else if (currentStaff.getStep().equals(STEP.ADDING_STAFF)) {
            if (message.getText().endsWith("Назад")) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId().toString());
                sendMessage.setText("Вы вернулись в главное меню! ⬅\uFE0F");
                execute(sendMessage);
                updateSTEP(STEP.START);
                start(message);
            } else {
                try {
                    String[] lines = message.getText().split("\\n");
                    String name = lines[0].split("\\.")[1].trim();
                    String phone = lines[1].split("\\.")[1].trim();
                    staffRepository.save(Staff.builder()
                            .id(null)
                            .name(name.replaceAll(" ", ""))
                            .phone(phone.replaceAll(" ", ""))
                            .role(Role.STAFF)
                            .step(STEP.START)
                            .chatId(null)
                                    .staffStatus(StaffStatus.ACTIVE)
                            .build());
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(message.getChatId().toString());
                    sendMessage.setText("Персонал был сохранен \uD83E\uDDD1\u200D\uD83D\uDCBC: \n" +
                            "имя - " + name + "\n" +
                            "телефон - " + phone);
                    execute(sendMessage);
                    updateSTEP(STEP.START);
                } catch (ArrayIndexOutOfBoundsException e) {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setText("пожалуйста, введите данные в правильном формате! Вы вернулись в главное меню! \uD83D\uDCC3 ");
                    sendMessage.setChatId(message.getChatId().toString());
                    execute(sendMessage);
                    updateSTEP(STEP.START);
                }
            }
        } else if (message.getText().startsWith("Добавить кредит") | currentStaff.getStep().equals(STEP.BACK_ADD_CREDIT)) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText("Отправить информацию в формате \uD83D\uDCC4: \n " +
                    "1.Имя клиента \uD83D\uDC64\n" +
                    "2.Сумма \uD83D\uDCB5\n" +
                    "3.Дата окончания (01.01.2024) \uD83D\uDCC5\n" +
                    "\n" +
                    "После того, как вся информация будет заполнена, вам следует подобрать для нее персонал! \uD83E\uDDD1\u200D\uD83D\uDCBC");
            execute(sendMessage);
            updateSTEP(STEP.ADDING_CREDIT);
        } else if (currentStaff.getStep().equals(STEP.ADDING_CREDIT)) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            String[] lines = message.getText().split("\\n");
            String clientName = "";
            String amount = "";
            String endDate = "";
            try {
                clientName = lines[0].split("\\.")[1].trim();
                amount = lines[1].split("\\.")[1].trim();
                endDate = extractDate(message.getText());
            } catch (ArrayIndexOutOfBoundsException e) {
                SendMessage sendMessage1 = new SendMessage();
                sendMessage1.setChatId(message.getChatId().toString());
                sendMessage1.setText("Некорректное формат!");
                execute(sendMessage1);
                return;
            }
            if (validateDate(endDate) == null) {
                SendMessage sendMessageErr = new SendMessage();
                sendMessageErr.setChatId(message.getChatId().toString());
                sendMessageErr.setText("Отправте реальную дату! e.g(01.01.2024)");
                execute(sendMessageErr);
            } else {
                Date date = validateDate(endDate);
                if (!isFutureDate(date)) {
                    SendMessage sendMessageErr = new SendMessage();
                    sendMessageErr.setChatId(message.getChatId().toString());
                    sendMessageErr.setText("Отправте реальную дату! e.g(01.01.2024)");
                    execute(sendMessageErr);
                } else {


                    String formatted = "";
                    try {
                        DecimalFormat formatter = new DecimalFormat("#,###");
                        formatted = formatter.format(Integer.parseInt(amount)).replace(',', '.');


                    } catch (NumberFormatException e) {
                        SendMessage sendMessageErr = new SendMessage();
                        sendMessageErr.setChatId(message.getChatId().toString());
                        sendMessageErr.setText("Некорректное формат!");
                        execute(sendMessageErr);
                        updateSTEP(STEP.ADDING_CREDIT);
                        return;
                    }


                    sendMessage.setText("Кредитная информация: \n" +
                            "\uD83D\uDD35 Имя клиента  - " + clientName + "\n" +
                            "\uD83D\uDCB0 Сумма - " + formatted + " Сум \n" +
                            "\uD83D\uDCC5 Дата окончания - " + endDate + "\n" +
                            "\n" +
                            "Приступить к выбору персонала?");

                    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                    List<InlineKeyboardButton> buttons = new ArrayList<>();

                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();

                    inlineKeyboardButton.setText("Да ✅");
                    inlineKeyboardButton.setCallbackData("YES_CONFIRM_INFO");

                    InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();


                    inlineKeyboardButton1.setText("Нет ❌");
                    inlineKeyboardButton1.setCallbackData("NO_CONFIRM_INFO");
                    buttons.add(inlineKeyboardButton);
                    buttons.add(inlineKeyboardButton1);

                    inlineKeyboardMarkup.setKeyboard(List.of(buttons));

                    sendMessage.setReplyMarkup(inlineKeyboardMarkup);

                    execute(sendMessage);


                    updateSTEP(STEP.CHECK_CONFIRMING);

                    newCredit.setAmount(Integer.parseInt(amount));
                    newCredit.setClient_name(clientName);
                    newCredit.setEndDate(date);
                }

            }
        } else if (currentStaff.getStep().equals(STEP.START)) {
            start(message);
        }
    }

    private static String extractDate(String input) {
        // Pattern to match "3." followed by a date in dd.MM.yyyy format
        Pattern datePattern = Pattern.compile("3\\.\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");
        Matcher matcher = datePattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).trim(); // Extract the date part
        }
        return null; // Return null if no date is found
    }

    public void starting(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("Поделитесь, пожалуйста, контактом.");

        // Create a keyboard with a contact request button
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton button = new KeyboardButton();
        button.setText("Поделиться контактом");
        button.setRequestContact(true); // This flag requests contact
        row.add(button);
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void updateSTEP(STEP step) {
        currentStaff.setStep(step);
        staffRepository.save(currentStaff);
    }

    private Date validateDate(String dateStr) {
        try {
            return dateFormatter.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    private boolean isFutureDate(Date date) {
        return date.after(new Date());
    }


    @SneakyThrows
    void start(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton button = new KeyboardButton();
        button.setText("Добавить сотрудника! \uD83E\uDDD1\u200D\uD83D\uDCBC");
        KeyboardButton button2 = new KeyboardButton();
        button2.setText("Добавить кредит \uD83D\uDCB8");
        row.add(button);
        row.add(button2);
        KeyboardRow line = new KeyboardRow();
        KeyboardRow lastLine = new KeyboardRow();
        KeyboardButton removeStaff = new KeyboardButton();
        removeStaff.setText("Удаление персонала \uD83E\uDDD1\u200D\uD83D\uDCBC");
        KeyboardButton button1 = new KeyboardButton();
        button1.setText("Cтатистика \uD83D\uDCC8");
        KeyboardButton buttonStaff = new KeyboardButton();
        buttonStaff.setText("Сотрудники \uD83E\uDDD1\u200D\uD83D\uDCBC");
        line.add(button1);
        lastLine.add(removeStaff);
        line.add(buttonStaff);
        keyboard.add(row);
        keyboard.add(line);
        keyboard.add(lastLine);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setText("Здравствуйте, " + currentStaff.getName() + " \uD83D\uDC4B!");
        execute(sendMessage);
    }

    @SneakyThrows
    private Staff isStaff(String phone, Long chatId) {
        Optional<Staff> byPhone = staffRepository.findByPhoneAndStaffStatus(phone,StaffStatus.ACTIVE);
        if (byPhone.isPresent()) {
            return byPhone.get();
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Мы не можем найти сотрудников, соответствующих вашему номеру \uD83D\uDEAB, попробуйте еще раз позже!");
        execute(sendMessage);
        return null;
    }


    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

}