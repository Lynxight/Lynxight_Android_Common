package com.lynxight.common.Reports;

public class BugsnagReport extends Exception {
    public BugsnagReport(String reportMessage) {
        super(reportMessage);
    }
}
