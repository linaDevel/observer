package ru.linachan.observer;

import ru.linachan.common.GenericCore;
import ru.linachan.common.GenericWorker;
import ru.linachan.common.utils.Utils;
import ru.linachan.observer.db.DataBaseWrapper;

public class ObserverWorker implements GenericWorker {

    private static DataBaseWrapper dataBaseWrapper = new DataBaseWrapper(
        GenericCore.instance().config().getString("db.uri", "mongodb://127.0.0.1:27017/"),
        GenericCore.instance().config().getString("db.name", "observer")
    );

    @Override
    public void onInit() {

    }

    @Override
    public void onShutDown() {
        dataBaseWrapper.close();
    }

    @Override
    public void run() {
        while (GenericCore.instance().running()) {
            Utils.sleep(100);
        }
    }

    public static DataBaseWrapper db() {
        return dataBaseWrapper;
    }
}
