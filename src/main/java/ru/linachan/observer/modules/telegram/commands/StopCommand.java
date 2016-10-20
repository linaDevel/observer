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

public class StopCommand extends BotCommand {

    private MongoCollection<Document> users = ObserverWorker.db().collection("users");

    private static Logger logger = LoggerFactory.getLogger(StopCommand.class);

    public StopCommand() {
        super("stop", "Cancel registration and stop notifications.");

    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long count = users.deleteOne(new Document("chat", chat.getId())).getDeletedCount();

        if (count > 0) {
            SendMessage sendMessage = new SendMessage();

            sendMessage.setChatId(chat.getId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText("Registration cancelled. Notifications disabled.");


            try {
                absSender.sendMessage(sendMessage);
            } catch (TelegramApiException e) {
                logger.error("Unable to send message to {}: {}", chat.getId(), e.getMessage());
            }
        }
    }
}
