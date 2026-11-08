package ge.qa.tests;

import ge.qa.mock.WireMockStubs;
import org.testng.annotations.BeforeSuite;

public abstract class BaseTest {

    /** Registers all stubs in WireMock before any test runs. */
    @BeforeSuite(alwaysRun = true)
    public void setUpStubs() {
        WireMockStubs.register();
    }
}
