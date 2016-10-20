package ru.linachan.observer.modules.watch.data.build;

import java.util.regex.Pattern;

public class BuildErrorPattern {

    // Typical error patterns
    public static final Pattern FAILED_NOOP_TASK = Pattern.compile(
        "^(?m)(?<state>FAILED|SUCCESS)\\s+(?<task>[^\\s]+)\\s+(?<os>[^\\s]+)\\s+(?<yaml>[^\\s]+)\\s*$"
    );
    public static final Pattern ASSERTION_ERROR = Pattern.compile(
        "^(?m)(TimeoutError|AssertionError):\\s*(?<error>.*?)\\s*$"
    );
    public static final Pattern GENERIC_ERROR = Pattern.compile(
        "^(?mi)(error|fatal):\\s*(?<error>.*?)\\s*$"
    );

    // Additional info patterns
    public static final Pattern FAILED_NOOP_TEST = Pattern.compile(
        "^(?m)\\s*(?<state>failed|success)\\s*(?<test>should .*?)$"
    );

    public static final Pattern FAILED_OSTF_TEST = Pattern.compile(
        "^(?m)\\s+-\\s+(?<test>.*?)\\s*$"
    );
}
