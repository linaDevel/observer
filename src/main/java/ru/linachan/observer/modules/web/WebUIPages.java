package ru.linachan.observer.modules.web;


import ru.linachan.observer.modules.web.handler.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebUIPages {

    private static List<WebUIPage> webUIPages = new ArrayList<>();
    private static WebUIPage defaultPage = WebUIPage.newPage("404", null, "Page Not Found", null, new DefaultHandler());

    static {
        webUIPages.add(WebUIPage.newPage("index", "/", "Overview", "dashboard", new Index()));
        webUIPages.add(WebUIPage.newPage("report", "/report/", "Reports", "report_problem", new Report()));
        webUIPages.add(WebUIPage.newPage("job", "/job/", "Jobs", "autorenew", new Job()));
    }

    public static class WebUIPage {

        private String view;
        private String link;
        private String title;
        private String icon;
        private String template;
        private String script;

        private WebUIPageHandler handler;

        public WebUIPage(String view, String link, String title, String icon, String template, String script, WebUIPageHandler handler) {
            this.view = view;
            this.link = link;
            this.title = title;
            this.icon = icon;
            this.template = template;
            this.script = script;
            this.handler = handler;
        }

        public String view() {
            return view;
        }

        public String link() {
            return link;
        }

        public String title() {
            return title;
        }

        public String icon() {
            return icon;
        }

        public String template() {
            return template;
        }

        public String script() {
            return script;
        }

        public WebUIPageHandler handler() {
            return handler;
        }

        public static WebUIPage newPage(String view, String link, String title, String icon, WebUIPageHandler handler, String template, String script) {
            return new WebUIPage(view, link, title, icon, template, script, handler);
        }

        public static WebUIPage newPage(String view, String link, String title, String icon, WebUIPageHandler handler) {
            return new WebUIPage(view, link, title, icon, String.format("%s.vm", view), String.format("%s.js", view), handler);
        }
    }

    public static WebUIPage getWebPage(String view) {
        Optional<WebUIPage> webUIPageOptional = webUIPages.stream()
            .filter(webUIPage -> webUIPage.view().equals(view)).findAny();

        if (webUIPageOptional.isPresent()) {
            return webUIPageOptional.get();
        }

        return defaultPage;
    }

    public static List<WebUIPage> getPages() {
        return webUIPages;
    }
}
