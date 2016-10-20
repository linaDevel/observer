package ru.linachan.observer.modules.web.handler;

import org.apache.velocity.VelocityContext;
import ru.linachan.common.GenericCore;
import ru.linachan.observer.modules.web.WebUIPageHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Job implements WebUIPageHandler {

    @Override
    public void prepareContext(VelocityContext ctx) {
        List<String> jobList = GenericCore.instance().config().getKeys("jenkins.job.").stream()
            .map(key -> key.replace("jenkins.job.", ""))
            .collect(Collectors.toList());

        Map<String, Integer> jobHistory = new HashMap<>();

        jobList.forEach(job -> jobHistory.put(job, GenericCore.instance().config().getInt("jenkins.job." + job, 10)));

        ctx.internalPut("JOBS", jobHistory);
    }
}
