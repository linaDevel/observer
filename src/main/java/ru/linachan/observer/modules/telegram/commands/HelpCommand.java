package ru.linachan.observer.modules.telegram.commands;

import org.apache.log4j.helpers.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commands.BotCommand;

import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.linachan.observer.modules.telegram.TelegramBot;

public class HelpCommand extends BotCommand {

    private TelegramBot bot;

    private static Logger logger = LoggerFactory.getLogger(HelpCommand.class);

    public HelpCommand(TelegramBot bot) {
        super("help", "List all available commands");

        this.bot = bot;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        StringBuilder helpMessageBuilder = new StringBuilder("<b>Help</b>\n");
        helpMessageBuilder.append("List of available commands:\n\n");

        for (BotCommand botCommand : bot.commands()) {
            helpMessageBuilder.append(botCommand.toString()).append("\n\n");
        }

        SendMessage helpMessage = new SendMessage();
        helpMessage.setChatId(chat.getId().toString());
        helpMessage.enableHtml(true);
        helpMessage.setText(helpMessageBuilder.toString());

        try {
            absSender.sendMessage(helpMessage);
        } catch (TelegramApiException e) {
            logger.error("Unable to send response: {}", e.getMessage());
        }
    }
}
