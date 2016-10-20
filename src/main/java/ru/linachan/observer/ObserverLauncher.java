package ru.linachan.observer;

import ru.linachan.common.GenericCore;
import ru.linachan.observer.component.ComponentManager;
import ru.linachan.observer.service.ServiceManager;

public class ObserverLauncher {

    public static void main(String[] args) {
        GenericCore.setConfigName("observer.ini");
        final GenericCore core = GenericCore.instance(args);

        core.worker(new ObserverWorker());

        core.manager(ServiceManager.class);
        core.manager(ComponentManager.class);

        core.mainLoop();
    }
}
