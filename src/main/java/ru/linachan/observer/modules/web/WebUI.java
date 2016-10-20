package ru.linachan.observer.modules.web;

import ru.linachan.common.GenericServer;
import ru.linachan.observer.component.Component;
import ru.linachan.observer.modules.web.websocket.WebSocketChannelInitializer;

public class WebUI implements Component {

    private GenericServer webServer;

    @Override
    public void onInit() {
        String webServerHost = core.config().getString("web.host", "0.0.0.0");
        Integer webServerPort = core.config().getInt("web.port", 8080);

        webServer = new GenericServer(webServerHost, webServerPort);
        webServer.setChannelHandler(new WebSocketChannelInitializer());

        core.execute(webServer);
    }

    @Override
    public void onShutDown() {
        webServer.stop();
    }
}
