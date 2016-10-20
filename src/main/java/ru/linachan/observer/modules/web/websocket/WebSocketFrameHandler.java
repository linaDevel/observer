package ru.linachan.observer.modules.web.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.linachan.common.GenericDispatcher;
import ru.linachan.observer.modules.web.WebUIPageHandler;
import ru.linachan.observer.modules.web.WebUIPages;


public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketFrameHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            JSONObject request = (JSONObject) new JSONParser().parse(((TextWebSocketFrame) frame).text());

            JSONObject response = handleRequest(request);

            ctx.channel().writeAndFlush(new TextWebSocketFrame(response.toJSONString()));
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject handleRequest(JSONObject request) {
        JSONObject response = new JSONObject();

        String action = (String) request.getOrDefault("action", null);
        String view = (String) request.getOrDefault("view", null);

        WebUIPageHandler handler = WebUIPages.getWebPage(view).handler();

        response.put("view", view);
        response.put("type", action);

        GenericDispatcher.dispatch(handler, request, response);

        return response;
    }

}