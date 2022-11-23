package ru.springLearning.SpringDemoBot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.springLearning.SpringDemoBot.config.BotConfig;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig cfg;

    public TelegramBot(BotConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public String getBotUsername() {
        return cfg.getBotName();
    }

    @Override
    public String getBotToken() {
        return cfg.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String msg = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (msg) {
                case "/start":
                    startCommandRecieved(chatId, update.getMessage().getChat().getFirstName());
                    break;
                default:
                    sendMessage(chatId, msg + " - this command is not recognized!");
                    break;
            }
        }
    }

    private void startCommandRecieved(long chatId, String name) {
        String answer = "Hi, " + name + ", nice to meet you!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message);
        } catch (TelegramApiException e) {
        }

    }
}
