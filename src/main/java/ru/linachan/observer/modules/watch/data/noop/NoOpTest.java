package ru.linachan.observer.modules.watch.data.noop;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NoOpTest {

    private String taskName;
    private String configurationName;
    private String osName;

    private NoOpTestState testState;

    private List<NoOpTestResult> testResults = new ArrayList<>();

    private static final Pattern PATTERN_TEST_RESULT = Pattern.compile(
        "^(?<test>.*?)\\s*(?<cause>\\(.*?\\))?"
    );

    public NoOpTest(String task, String configuration, String os, String state) {
        taskName = task;
        configurationName = configuration;
        osName = os;

        switch (state.toLowerCase()) {
            case "success":
                testState = NoOpTestState.SUCCESS;
                break;
            case "failed":
                testState = NoOpTestState.FAILED;
                break;
            default:
                testState = NoOpTestState.UNKNOWN;
                break;
        }
    }

    public void test(String test, String state) {
        Matcher testResultMatcher = PATTERN_TEST_RESULT.matcher(test);

        if (testResultMatcher.matches()) {
            testResults.add(
                new NoOpTestResult(
                    testResultMatcher.group("test"), state,
                    testResultMatcher.group("cause")
                )
            );
        }
    }

    public String task() {
        return taskName;
    }

    public String configuration() {
        return configurationName;
    }

    public String os() {
        return osName;
    }

    public NoOpTestState state() {
        return testState;
    }

    public List<NoOpTestResult> tests() {
        return testResults;
    }

    public List<NoOpTestResult> failedTests() {
        return testResults.stream()
            .filter(testResult -> testResult.state().equals(NoOpTestState.FAILED))
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("Task[%s]: %d failed tests", taskName, failedTests().size());
    }

    public Document toBSON() {
        return new Document("task", taskName)
            .append("yaml", configurationName)
            .append("os", osName)
            .append("state", testState.toString())
            .append("results", testResults.stream()
                .map(NoOpTestResult::toBSON)
                .collect(Collectors.toList())
            );
    }

    @SuppressWarnings("unchecked")
    public static NoOpTest fromBSON(Document noOpTestResult) {
        NoOpTest noOpTest = new NoOpTest(
            noOpTestResult.getString("task"),
            noOpTestResult.getString("yaml"),
            noOpTestResult.getString("os"),
            noOpTestResult.getString("state")
        );

        noOpTest.testResults.addAll(
            ((List<Document>) noOpTestResult.get("results")).stream()
                .map(NoOpTestResult::fromBSON)
                .collect(Collectors.toList())
        );

        return noOpTest;
    }
}