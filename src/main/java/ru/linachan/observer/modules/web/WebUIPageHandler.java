package ru.linachan.observer.modules.web;

import org.apache.velocity.VelocityContext;

public interface WebUIPageHandler {

    void prepareContext(VelocityContext ctx);

}
