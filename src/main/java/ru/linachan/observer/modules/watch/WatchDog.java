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
    private MongoCollection<Document> builds;

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
    }

    @Override
    public void run() {
        while (running) {
            BuildWithDetails build = failedBuilds.pop();
            if (build != null) {
                boolean buildFound = builds().count(
                    new Document("buildId", Integer.parseInt(build.getFullDisplayName().split(" #")[1]))
                        .append("jobName", build.getFullDisplayName().split(" #")[0])
                ) > 0;

                if (!buildFound) {
                    logger.info("New failed build found: {}", build.getId());

                    try {
                        BuildData buildData = new BuildData(build);
                        builds().insertOne(buildData.toBSON());
                    } catch (IOException e) {
                        logger.error("Unable to get Build details: {}", e.getMessage());
                    }
                }
            }
        }
    }

    public MongoCollection<Document> builds() {
        if (builds != null)
            return builds;

        builds = ((ObserverWorker) GenericCore.instance().worker()).db().collection("builds");
        return builds;
    }

    @Override
    public void onShutDown() {
        scheduler.shutdown();
        running = false;
    }
}
