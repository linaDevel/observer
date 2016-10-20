package ru.linachan.observer.modules.watch;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.common.GenericCore;
import ru.linachan.common.utils.Queue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JobWatch implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(JobWatch.class);

    private Queue<BuildWithDetails> failedBuilds;
    private JenkinsServer jenkinsServer;

    public JobWatch(JenkinsServer jenkinsServer) {
        this.failedBuilds = GenericCore.instance().queue(BuildWithDetails.class, "failedBuilds");
        this.jenkinsServer = jenkinsServer;
    }

    @Override
    public void run() {
        List<String> jobList = GenericCore.instance().config().getKeys("jenkins.job.").stream()
            .map(key -> key.replace("jenkins.job.", ""))
            .collect(Collectors.toList());

        Map<String, Integer> jobHistory = new HashMap<>();

        jobList.forEach(job -> jobHistory.put(job, GenericCore.instance().config().getInt("jenkins.job." + job, 10)));

        try {
            Map<String, Job> jobMap = jenkinsServer.getJobs();

            for (String jobName: jobList) {
                if (jobMap.containsKey(jobName)) {
                    JobWithDetails job = jobMap.get(jobName).details();

                    int lastBuildID = job.getLastCompletedBuild().getNumber();
                    int rangeStartID = Math.max(lastBuildID - jobHistory.get(jobName), 1);

                    logger.info("Job: {} LastBuildID: {}", job.getDisplayName(), lastBuildID);

                    for (int buildId = rangeStartID; buildId <= lastBuildID; buildId++) {
                        BuildWithDetails buildInfo = job.getBuildByNumber(buildId).details();

                        if (buildInfo.getResult().equals(BuildResult.FAILURE)) {
                            failedBuilds.push(buildInfo);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Unable to get Jenkins jobs: {}", e.getMessage());
        }
    }
}
