package ru.linachan.observer.modules.telegram;

import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.bots.commands.BotCommand;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.BotSession;
import ru.linachan.observer.component.Component;
import ru.linachan.observer.modules.telegram.commands.DoNotDisturbCommand;
import ru.linachan.observer.modules.telegram.commands.HelpCommand;
import ru.linachan.observer.modules.telegram.commands.StartCommand;
import ru.linachan.observer.modules.telegram.commands.StopCommand;

public class TelegramNotify implements Component {

    private BotSession session;
    private TelegramBot bot;

    @Override
    public void onInit() {
        bot = new TelegramBot();

        bot.register(new HelpCommand(bot));
        bot.register(new StartCommand());
        bot.register(new StopCommand());
        bot.register(new DoNotDisturbCommand());

        TelegramBotsApi api = new TelegramBotsApi();
        try {
            session = api.registerBot(bot);
        } catch (TelegramApiRequestException e) {
            logger.error("Unable to init Telegram Bot: {}", e.getMessage());
        }
    }

    public boolean register(BotCommand command) {
        return bot.register(command);
    }

    public void send(String message) {
        bot.send(message);
    }

    @Override
    public void onShutDown() {
        if (session != null) {
            session.close();
        }
    }
}
