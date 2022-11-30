package ru.springLearning.SpringDemoBot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.springLearning.SpringDemoBot.config.BotConfig;
import ru.springLearning.SpringDemoBot.model.Ads;
import ru.springLearning.SpringDemoBot.model.AdsRepository;
import ru.springLearning.SpringDemoBot.model.User;
import ru.springLearning.SpringDemoBot.model.UserRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdsRepository adsRepository;

    final BotConfig cfg;
    private static final String HELP_TEXT = "This bot  is created to demonstrate Spring capabilities.\n" + "You can execute commands from the menu on the left or by typing a command:\n\n" + "Type /start to see welcome message\n\n" + "Type /mydata to see datastore about yourself\n\n" + "Type /help to see this message\n\n" + "Type /deletedata to delete your data from the bot storage\n\n" + "Type /settings to see and define the bot setting\n\n" + "Type /register to registered\n\n" + "Type /send <*_YOUR_MESSAGE_*> to send your message to all subscribers";
    private static final String yesButton = "YES_BUTTON";
    private static final String noButton = "NO_BUTTON";
    private static final String ERROR_TEXT = "Error occurred: ";

    public TelegramBot(BotConfig cfg) {
        this.cfg = cfg;
        List listOfCommand = new ArrayList<>();
        listOfCommand.add(new BotCommand("/start", "get a welcome message"));
        listOfCommand.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommand.add(new BotCommand("/deletedata", "delete your data"));
        listOfCommand.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommand.add(new BotCommand("/settings", "set your preferences"));
        listOfCommand.add(new BotCommand("/send", "send message to all subscribers"));
        listOfCommand.add(new BotCommand("/register", "registered the user"));
        try {
            this.execute(new SetMyCommands(listOfCommand, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
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

            if (msg.contains("/send") && cfg.getAdminId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(msg.substring(msg.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {

                switch (msg) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case "/register":
                        register(chatId);
                        break;
                    default:
                        prepareAndSendMessage(chatId, msg + " - this command is not recognized!");
                        break;
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (callBackData.equals(yesButton)) {
                String text = "You pressed YES button";
                executeEditMessageText(chatId, text, messageId);
            } else if (callBackData.equals(noButton)) {
                String text = "You pressed NO button";
                executeEditMessageText(chatId, text, messageId);
            }
        }
    }

    private void executeEditMessageText(long chatId, String text, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    //register method send the message with button menu
    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var yButton = new InlineKeyboardButton();
        yButton.setText("Yes");
        yButton.setCallbackData(yesButton);
        var nBotton = new InlineKeyboardButton();
        nBotton.setText("No");
        nBotton.setCallbackData(noButton);
        rowInline.add(yButton);
        rowInline.add(nBotton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        executeMessage(message);
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        //check the emoji code on emojipedia.org
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + ":blush:");
        log.info("Replied to user: " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        /*==========Keyboard of bot menu==============*/
        //class for markup bot menu
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        //list of menu buttons
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        //first row of menu
        KeyboardRow row = new KeyboardRow();
        row.add("weather");
        row.add("get random joke");
        keyboardRows.add(row);
        //second row of menu
        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("delete my data");
        //save rows in list
        keyboardRows.add(row);
        //set list in bot menu object
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        executeMessage(message);

    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    @Scheduled(cron = "${cron.scheduler}")
    private void sendAds() {
        var ads = adsRepository.findAll();
        var users = userRepository.findAll();
        for (Ads ad : ads) {
            for (User user : users) {
                prepareAndSendMessage(user.getChatId(), ad.getAd());
            }
        }
    }
}
