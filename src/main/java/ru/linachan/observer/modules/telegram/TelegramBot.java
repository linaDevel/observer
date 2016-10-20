package ru.linachan.observer.modules.telegram;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.commands.BotCommand;
import org.telegram.telegrambots.bots.commands.CommandRegistry;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.linachan.common.GenericCore;
import ru.linachan.observer.ObserverWorker;

import java.util.Collection;

public class TelegramBot extends TelegramLongPollingBot {

    private CommandRegistry registry;
    private MongoCollection<Document> users;

    private static Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    public TelegramBot() {
        registry = new CommandRegistry(
            GenericCore.instance().config().getBoolean("telegram.bot.command_with_username", true),
            getBotUsername()
        );

        users = ObserverWorker.db().collection("users");
    }

    public boolean register(BotCommand command) {
        return registry.register(command);
    }

    public Collection<BotCommand> commands() {
        return registry.getRegisteredCommands();
    }

    public void send(String message) {
        for (Document user: users.find()) {
            Long doNotDisturb = user.getLong("doNotDisturb");
            if (doNotDisturb < System.currentTimeMillis()) {
                SendMessage sendMessage = new SendMessage();

                sendMessage.setChatId(String.valueOf(user.getLong("chat")));
                sendMessage.enableHtml(true);
                sendMessage.setText(message);

                try {
                    this.sendMessage(sendMessage);
                } catch (TelegramApiException e) {
                    logger.error("Unable to send message to {}: {}", user.getLong("chat"), e.getMessage());
                }
            }
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();

            if (message.isCommand()) {
                registry.executeCommand(this, message);
            }

            logger.info(update.toString());
        }
    }

    @Override
    public String getBotUsername() {
        return GenericCore.instance().config().getString("telegram.bot.name", null);
    }

    @Override
    public String getBotToken() {
        return GenericCore.instance().config().getString("telegram.bot.token", null);
    }

    @Override
    public void onClosing() {
        logger.info("Telegram Bot is going to shutdown");
    }
}
