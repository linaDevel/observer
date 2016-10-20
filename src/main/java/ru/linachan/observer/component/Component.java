package ru.linachan.observer.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.linachan.common.GenericCore;

public interface Component {

    GenericCore core = GenericCore.instance();
    Logger logger = LoggerFactory.getLogger(Component.class);

    void onInit();
    void onShutDown();

}
