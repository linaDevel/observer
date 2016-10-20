package ru.linachan.observer.modules.watch.data.noop;

import org.bson.Document;

public class NoOpTestResult {

    private String testName;
    private String failureReason;

    private NoOpTestState testState;

    public NoOpTestResult(String testCase, String state, String cause) {
        testName = testCase;
        failureReason = cause;

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

    public String name() {
        return testName;
    }

    public String reason() {
        return failureReason;
    }

    public NoOpTestState state() {
        return testState;
    }

    public Document toBSON() {
        return new Document("test", testName)
            .append("cause", failureReason)
            .append("state", testState.toString());
    }

    public static NoOpTestResult fromBSON(Document testResult) {
        return new NoOpTestResult(
            testResult.getString("test"),
            testResult.getString("state"),
            testResult.getString("cause")
        );
    }
}
