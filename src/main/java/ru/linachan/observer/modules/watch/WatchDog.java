package ru.linachan.observer.modules.watch;

import com.mongodb.client.MongoCollection;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.BuildWithDetails;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.common.GenericCore;
import ru.linachan.common.utils.Queue;
import ru.linachan.observer.ObserverWorker;
import ru.linachan.observer.component.ComponentManager;
import ru.linachan.observer.modules.telegram.TelegramNotify;
import ru.linachan.observer.modules.watch.data.build.BuildData;
import ru.linachan.observer.service.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WatchDog implements Service {

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private Queue<BuildWithDetails> failedBuilds;
    private MongoCollection<Document> builds = ObserverWorker.db().collection("builds");;

    private TelegramNotify bot;

    private boolean running = true;

    private static Logger logger = LoggerFactory.getLogger(WatchDog.class);

    @Override
    public void onInit() {
        failedBuilds = GenericCore.instance().queue(BuildWithDetails.class, "failedBuilds");

        URI jenkinsURI;

        try {
            jenkinsURI = new URI(
                GenericCore.instance().config().getString("jenkins.url", "http://localhost/jenkinsServer")
            );
        } catch (URISyntaxException e) {
            logger.error("Unable to parse Jenkins URL. Deactivating plugin...");
            return;
        }

        String jenkinsUserName = GenericCore.instance().config().getString("jenkins.username", null);
        String jenkinsPassWord = GenericCore.instance().config().getString("jenkins.password", null);

        JenkinsServer jenkinsServer;

        if ((jenkinsUserName != null)&&(jenkinsPassWord != null)) {
            jenkinsServer = new JenkinsServer(jenkinsURI, jenkinsUserName, jenkinsPassWord);
        } else {
            jenkinsServer = new JenkinsServer(jenkinsURI);
        }

        scheduler.scheduleAtFixedRate(
            new JobWatch(jenkinsServer), 0,
            GenericCore.instance().config().getLong("jenkins.watch_interval", 120L),
            TimeUnit.SECONDS
        );

        bot = GenericCore.instance()
            .manager(ComponentManager.class)
            .get(TelegramNotify.class);
    }

    @Override
    public void run() {
        while (running) {
            BuildWithDetails build = failedBuilds.pop();
            if (build != null) {
                boolean buildFound = builds.count(
                    new Document("buildId", Integer.parseInt(build.getFullDisplayName().split(" #")[1]))
                        .append("jobName", build.getFullDisplayName().split(" #")[0])
                ) > 0;

                if (!buildFound) {
                    logger.info("New failed build found: {}", build.getId());

                    try {
                        BuildData buildData = new BuildData(build);

                        StringBuilder report = new StringBuilder();

                        report.append("<b>New failed build</b>\n\n");
                        report.append(String.format("<b>Job:</b> %s\n", buildData.jobName()));
                        report.append(String.format(
                            "<b>Build:</b> <a href='%s'>%s</a>\n", buildData.url(), buildData.buildId()
                        ));

                        if (buildData.triggeredBy().equals("timer")) {
                            report.append("<b>Triggered by:</b> Timer\n");
                        } else {
                            report.append(String.format(
                                "<b>Triggered by:</b> <a href='%s'>CR #%s,%s: %s</a>\n", buildData.triggeredBy(),
                                buildData.changeId(), buildData.patchSet(), buildData.changeTitle()
                            ));
                        }

                        report.append(String.format("<b>Time:</b> %s\n", buildData.time()));

                        bot.send(report.toString());

                        builds.insertOne(buildData.toBSON());
                    } catch (IOException e) {
                        logger.error("Unable to get Build details: {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void onShutDown() {
        scheduler.shutdown();
        running = false;
    }
}
