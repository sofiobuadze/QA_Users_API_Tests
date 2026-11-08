package ge.qa.listener;

import ge.qa.db.ResultRepository;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects results per test method
 */
public class ResultListener implements ITestListener {

    private final ResultRepository repository = new ResultRepository();

    @Override
    public void onStart(ITestContext context) {
        repository.init();
    }

    @Override
    public void onFinish(ITestContext context) {
        Map<String, String> statuses = new LinkedHashMap<>();

        for (ITestResult r : context.getPassedTests().getAllResults()) {
            statuses.putIfAbsent(r.getMethod().getMethodName(), "PASSED");
        }
        for (ITestResult r : context.getFailedTests().getAllResults()) {
            statuses.put(r.getMethod().getMethodName(), "FAILED");
        }
        for (ITestResult r : context.getSkippedTests().getAllResults()) {
            statuses.put(r.getMethod().getMethodName(), "FAILED");
        }

        statuses.forEach(repository::save);
    }
}
