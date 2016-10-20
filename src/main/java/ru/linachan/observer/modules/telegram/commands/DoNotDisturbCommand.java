package ru.linachan.observer.modules.telegram.commands;

import com.google.common.base.Joiner;
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
import ru.linachan.observer.ObserverWorker;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DoNotDisturbCommand extends BotCommand {

    private MongoCollection<Document> users = ObserverWorker.db().collection("users");

    private static Logger logger = LoggerFactory.getLogger(DoNotDisturbCommand.class);
    private static final Pattern timePattern = Pattern.compile(
        "^((?<action>[a-z]+)|(?<time>[0-9]+)(?<unit>m|h|d))$"
    );

    public DoNotDisturbCommand() {
        super("dnd", "Temporarily disable notifications");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long count = users.count(new Document("chat", chat.getId()));

        if (count > 0) {
            SendMessage sendMessage = new SendMessage();

            sendMessage.setChatId(chat.getId().toString());
            sendMessage.enableHtml(true);

            if (strings.length > 0) {
                Matcher matcher = timePattern.matcher(strings[0]);
                if (matcher.matches()) {
                    Long dnd = 0L;

                    if (matcher.group("action") != null) {
                        switch (matcher.group("action")) {
                            case "cancel":
                                dnd = 0L;
                                sendMessage.setText("Do Not Disturb mode cancelled");
                                break;
                            case "status":
                                dnd = users.find(new Document("chat", chat.getId())).first().getLong("doNotDisturb");

                                if (dnd > System.currentTimeMillis()) {
                                    sendMessage.setText("Do Not Disturb mode active till <b>" + new Date(dnd).toString() + "</b>");
                                } else {
                                    sendMessage.setText("Do Not Disturb mode not active");
                                }
                                break;
                        }
                    } else {
                        Integer time = Integer.parseInt(matcher.group("time"));
                        String unit = matcher.group("unit");

                        dnd = System.currentTimeMillis();
                        switch (unit) {
                            case "m":
                                dnd += time * 60000L;
                                break;
                            case "h":
                                dnd += time * 3600000L;
                                break;
                            case "d":
                                dnd += time * 86400000L;
                                break;
                        }

                        sendMessage.setText("Do Not Disturb mode active till <b>" + new Date(dnd).toString() + "</b>");
                    }

                    users.updateOne(
                        new Document("chat", chat.getId()),
                        new Document("$set", new Document(
                            "doNotDisturb", dnd
                        ))
                    );
                } else {
                    sendMessage.setText("Invalid argument");
                }
            } else {
                sendMessage.setText("Usage: <b>/dnd</b> [cancel|status|[0-9]+(m|h|d)]]");
            }

            try {
                absSender.sendMessage(sendMessage);
            } catch (TelegramApiException e) {
                logger.error("Unable to send message to {}: {}", chat.getId(), e.getMessage());
            }
        }
    }
}
