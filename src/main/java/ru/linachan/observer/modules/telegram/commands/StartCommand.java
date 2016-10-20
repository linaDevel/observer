package ru.linachan.observer.modules.telegram.commands;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commands.BotCommand;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.linachan.common.GenericCore;
import ru.linachan.observer.ObserverWorker;

public class StartCommand extends BotCommand {

    private MongoCollection<Document> users = ObserverWorker.db().collection("users");

    private static Logger logger = LoggerFactory.getLogger(StartCommand.class);

    public StartCommand() {
        super("start", "Register with Observer Bot.");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long count = users.count(new Document("chat", chat.getId()));

        if (count == 0) {
            users.insertOne(new Document("chat", chat.getId()).append("doNotDisturb", 0L));

            SendMessage sendMessage = new SendMessage();

            sendMessage.setChatId(chat.getId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText("Registration completed. Notifications enabled.");


            try {
                absSender.sendMessage(sendMessage);
            } catch (TelegramApiException e) {
                logger.error("Unable to send message to {}: {}", chat.getId(), e.getMessage());
            }
        }
    }
}
