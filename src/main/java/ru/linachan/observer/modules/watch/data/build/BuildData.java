package ru.linachan.observer.modules.watch.data.build;

import com.offbytwo.jenkins.model.BuildWithDetails;
import org.bson.Document;
import org.bson.types.ObjectId;
import ru.linachan.observer.modules.watch.data.noop.NoOpTest;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class BuildData {

    private ObjectId id;

    private Integer buildId;
    private String jobName;
    private String description;
    private String url;

    private String changeTitle;
    private Integer changeId;
    private Integer patchSet;

    private String slave;

    private Long timeStamp;

    private List<String> bugs = new ArrayList<>();
    private String comment = "";

    private Map<String, String> properties = new HashMap<>();

    private List<String> deploymentErrors = new ArrayList<>();
    private List<NoOpTest> noOpTests = new ArrayList<>();

    public BuildData(BuildWithDetails buildDetails) throws IOException {
        buildId = Integer.parseInt(buildDetails.getFullDisplayName().split(" #")[1]);
        jobName = buildDetails.getFullDisplayName().split(" #")[0];
        description = buildDetails.getDescription();
        url = buildDetails.getUrl();

        if (buildDetails.getParameters().containsKey("GERRIT_CHANGE_SUBJECT")) {
            changeTitle = buildDetails.getParameters().get("GERRIT_CHANGE_SUBJECT");
            changeId = Integer.parseInt(buildDetails.getParameters().get("GERRIT_CHANGE_NUMBER"));
            patchSet = Integer.parseInt(buildDetails.getParameters().get("GERRIT_PATCHSET_NUMBER"));
        }

        slave = buildDetails.getBuiltOn();

        timeStamp = buildDetails.getTimestamp();

        parseConsoleOutput(buildDetails.getConsoleOutputText());
    }

    @SuppressWarnings("unchecked")
    private BuildData(Document buildDetails) {
        id = buildDetails.getObjectId("_id");

        buildId = buildDetails.getInteger("buildId");
        jobName = buildDetails.getString("jobName");
        description = buildDetails.getString("description");
        url = buildDetails.getString("url");

        changeTitle = ((Document) buildDetails.get("change")).getString("title");
        changeId = ((Document) buildDetails.get("change")).getInteger("id");
        patchSet = ((Document) buildDetails.get("change")).getInteger("patchSet");

        slave = buildDetails.getString("slave");

        timeStamp = buildDetails.getLong("timeStamp");

        bugs = (List<String>) buildDetails.get("bugs");
        comment = buildDetails.getString("comment");
        properties = (Map<String, String>) buildDetails.get("properties");

        deploymentErrors = (List<String>) ((Document) buildDetails.get("error")).get("deployment");

        noOpTests = ((List<Document>) ((Document) buildDetails.get("error")).get("noop")).stream()
            .map(NoOpTest::fromBSON).collect(Collectors.toList());
    }

    private void parseConsoleOutput(String consoleOutputText) {
        String[] consoleOutput = consoleOutputText.split("\\r\\n");

        for (int lineNo = 0; lineNo < consoleOutput.length; lineNo++) {
            String line = consoleOutput[lineNo];

            Matcher noOpTaskMatcher = BuildErrorPattern.FAILED_NOOP_TASK.matcher(line);
            if (noOpTaskMatcher.matches()) {
                NoOpTest noOpTest = new NoOpTest(
                    noOpTaskMatcher.group("task"),
                    noOpTaskMatcher.group("yaml"),
                    noOpTaskMatcher.group("os"),
                    noOpTaskMatcher.group("state")
                );

                for (int subLineNo = lineNo + 1; subLineNo < consoleOutput.length; subLineNo++) {
                    String subLine = consoleOutput[subLineNo];

                    Matcher noOpTestMatcher = BuildErrorPattern.FAILED_NOOP_TEST.matcher(subLine);
                    if (noOpTestMatcher.matches()) {
                        noOpTest.test(
                            noOpTestMatcher.group("test"),
                            noOpTestMatcher.group("state")
                        );
                    } else {
                        break;
                    }
                }

                noOpTests.add(noOpTest);
                continue;
            }

            Matcher assertionErrorMatcher = BuildErrorPattern.ASSERTION_ERROR.matcher(line);
            if (assertionErrorMatcher.matches()) {
                deploymentErrors.add(line);

                for (int subLineNo = lineNo + 1; subLineNo < consoleOutput.length; subLineNo++) {
                    String subLine = consoleOutput[subLineNo];

                    Matcher ostfTestMatcher = BuildErrorPattern.FAILED_OSTF_TEST.matcher(subLine);
                    if (ostfTestMatcher.matches()) {
                        deploymentErrors.add("OSTF Failed:" + subLine);
                    } else {
                        break;
                    }
                }
                continue;
            }

            Matcher genericErrorMatcher = BuildErrorPattern.GENERIC_ERROR.matcher(line);
            if (genericErrorMatcher.matches()) {
                deploymentErrors.add(line);
            }
        }
    }

    public void bug(String bug) {
        bugs.add(bug);
    }

    public void comment(String commentString) {
        comment = commentString;
    }

    public void property(String property, String value) {
        properties.put(property, value);
    }

    public Integer buildId() {
        return buildId;
    }

    public String jobName() {
        return jobName;
    }

    public String description() {
        return description;
    }

    public String url() {
        return url;
    }

    public String changeTitle() {
        return changeTitle;
    }

    public Integer changeId() {
        return changeId;
    }

    public Integer patchSet() {
        return patchSet;
    }

    public String slave() {
        return slave;
    }

    public Long timeStamp() {
        return timeStamp;
    }

    public String time() {
        return new Date(timeStamp).toString();
    }

    public String comment() {
        return comment;
    }

    public List<String> bugs() {
        return bugs;
    }

    public Map<String, String> properties() {
        return properties;
    }

    public String property(String property) {
        return properties.get(property);
    }

    public List<String> deploymentErrors() {
        return deploymentErrors;
    }

    public List<NoOpTest> noOpTests() {
        return noOpTests;
    }

    public String triggeredBy() {
        if (changeTitle != null)
            return String.format("https://review.openstack.org/#/c/%s/%s", changeId, patchSet);
        return "timer";
    }

    public Document toBSON() {
        Document bson = new Document();

        if (id != null) {
            bson.append("_id", id);
        }

        bson.append("buildId", buildId);
        bson.append("jobName", jobName);
        bson.append("description", description);
        bson.append("url", url);

        Document change = new Document();

        change.append("title", changeTitle);
        change.append("id", changeId);
        change.append("patchSet", patchSet);

        bson.append("change", change);

        bson.append("slave", slave);
        bson.append("timeStamp", timeStamp);

        bson.append("comment", comment);
        bson.append("bugs", bugs);
        bson.append("properties", properties);

        Document error = new Document();

        error.append("deployment", deploymentErrors);
        error.append("noop", noOpTests.stream().map(NoOpTest::toBSON).collect(Collectors.toList()));

        bson.append("error", error);

        return bson;
    }

    public static BuildData fromBSON(Document bson) {
        return new BuildData(bson);
    }

    public String id() {
        return id.toHexString();
    }
}
