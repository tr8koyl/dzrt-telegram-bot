package project.service;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

@Data
@Service
@Log4j
public class TelegramBotService extends TelegramLongPollingBot {

    @Lazy
    @Autowired
    private SubscriptionService subscriptionService;

    @Value("${bot_info.username}")
    private String botUsername;

    @Value("${bot_info.token}")
    private String botToken;

    @SneakyThrows
    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {

//        sendReplyKeyboard(update.getMessage().getChatId());
        subscriptionService.onUpdateReceived(update);

    }

    public void sendTextMessage(String text, long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(text);
        sendMessage.setChatId(chatId);
        sendMessage.enableMarkdown(true);

        try {
            execute(sendMessage);
        } catch (Exception e) {
            log.error("Error occurred: " + e.getMessage());
        }

    }

    public void sendReplyKeyboard(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("مرحبًا!");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("التحقق من توفر المنتجات"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("الاشتراک"));
        row2.add(new KeyboardButton("تاريخ تسجيلي"));
        keyboard.add(row2);
        keyboard.add(row1);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (Exception e) {
            log.error("Error occurred: " + e.getMessage());
        }

    }

}