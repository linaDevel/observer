package ru.linachan.observer.modules.web.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import ru.linachan.common.GenericCore;
import ru.linachan.observer.modules.web.WebUIPages;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class WebSocketPageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String webSocketPath;

    private final Pattern viewPattern = Pattern.compile("^/((?<view>[a-z0-9]+)/)?$");
    private final Pattern staticPattern = Pattern.compile("^/static/(?<type>[a-zA-Z0-9_]+)/(?<folder>([a-zA-Z0-9/]+)/)?(?<file>[^/]+)$");

    private final static VelocityEngine vEngine = new VelocityEngine();

    public WebSocketPageHandler(String wsPath) {
        webSocketPath = wsPath;

        String templatesFolder = GenericCore.instance().config().getString("web.templates_root", "templates");
        Properties props = new Properties();
        props.setProperty("file.resource.loader.path", templatesFolder);
        vEngine.init(props);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        if (req.method() != GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }


        Matcher viewMatcher = viewPattern.matcher(req.uri());
        Matcher staticMatcher = staticPattern.matcher(req.uri());

        if (viewMatcher.matches()) {
            String view = (viewMatcher.group("view") != null) ? viewMatcher.group("view") : "index";
            ByteBuf content = renderTemplate(view, getWebSocketLocation(req, webSocketPath));
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            HttpUtil.setContentLength(res, content.readableBytes());
            sendHttpResponse(ctx, req, res);
        } else if (staticMatcher.matches()) {
            String type = staticMatcher.group("type");
            String staticSubFolder = staticMatcher.group("folder");
            String staticFilePath = staticMatcher.group("file");

            String staticRoot = GenericCore.instance().config().getString("web.static_root", "static");

            File staticTypeRoot = new File(new File(staticRoot), type);
            File staticFile = new File(
                (staticSubFolder != null) ? new File(staticTypeRoot, staticSubFolder) : staticTypeRoot, staticFilePath
            );

            if (staticFile.exists() && staticFile.isFile()) {
                ByteBuf content = Unpooled.buffer();

                content.writeBytes(new FileInputStream(staticFile), (int) staticFile.length());

                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
                res.headers().set(HttpHeaderNames.CONTENT_TYPE, Files.probeContentType(staticFile.toPath()));
                HttpUtil.setContentLength(res, content.readableBytes());
                sendHttpResponse(ctx, req, res);
            } else {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            }
        } else {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
        }
    }

    private ByteBuf renderTemplate(String view, String webSocketLocation) {
        Template template = vEngine.getTemplate("main.vm");
        VelocityContext context = new VelocityContext();

        WebUIPages.WebUIPage templateData = WebUIPages.getWebPage(view);

        context.internalPut("PAGE_TEMPLATE", templateData.template());
        context.internalPut("PAGE_TITLE", templateData.title());
        context.internalPut("PAGE_SCRIPT", templateData.script());

        context.internalPut("PAGES", WebUIPages.getPages());
        context.internalPut("WS_PATH", webSocketLocation);
        context.internalPut("TS", System.currentTimeMillis());

        if (templateData.handler() != null) {
            templateData.handler().prepareContext(context);
        }

        StringWriter renderResult = new StringWriter();
        template.merge(context, renderResult);
        return Unpooled.copiedBuffer(renderResult.toString().getBytes());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {

        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
       }
   }

   private static String getWebSocketLocation(HttpRequest req, String path) {
       return "ws://" + req.headers().get(HttpHeaderNames.HOST) + path;
   }
}