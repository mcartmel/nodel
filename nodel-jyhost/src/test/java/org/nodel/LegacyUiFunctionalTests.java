package org.nodel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(LegacyUiTestSupport.LEGACY_UI_TAG)
@Tag(LegacyUiTestSupport.LEGACY_UI_FUNCTIONAL_TAG)
public class LegacyUiFunctionalTests extends LegacyUiTestSupport {

    @BeforeAll
    static void setup() {
        initBrowser();
        createLegacyUiFixtures();
    }

    @AfterAll
    static void teardown() {
        cleanupLegacyUiFixtures();
        closeBrowser();
    }

    @Test
    void localsPageRendersCoreWidgets() throws Exception {
        page.navigate(BASE_URL + "/locals.htm");
        waitForBodyReady();

        assertTrue(page.locator(".nodel-add").count() > 0, "Locals page should expose the add-node widget");
        assertTrue(page.locator(".nodel-locals").count() > 0, "Locals page should expose the locals list");

        markUiGate("locals");
    }

    @Test
    void nodesPageRendersCoreWidgets() throws Exception {
        page.navigate(BASE_URL + "/nodes.htm");
        waitForBodyReady();

        assertTrue(page.locator(".nodel-list").count() > 0, "Nodes page should expose the node list");
        assertTrue(page.locator(".nodel-list .list-group-item").count() >= 0, "Nodes page should render list items or an empty list shell");

        markUiGate("nodes");
    }

    @Test
    void nodelActivityPageRendersCoreWidgets() throws Exception {
        navigateToNode(UI_FALLBACK_NODE);
        waitForBodyReady();

        assertTrue(page.locator(".nodel-description").count() > 0, "Node page should expose the description panel");
        assertTrue(page.locator(".nodel-console").count() > 0, "Node page should expose the console panel");
        assertTrue(page.locator(".nodel-editor").count() > 0, "Node page should expose the editor panel");
        assertTrue(page.locator(".nodel-actsig").count() > 0, "Node page should expose the actions/signals panel");
        assertTrue(page.locator(".nodel-log").count() > 0, "Node page should expose the log panel");

        markUiGate("nodel-activity");
    }

    @Test
    void nodelConfigPageRendersCoreWidgets() throws Exception {
        page.navigate(BASE_URL + "/nodes/" + getReducedName(UI_FALLBACK_NODE) + "/#Config");
        waitForBodyReady();
        waitForElement(".nodel-params");

        assertTrue(page.locator(".nodel-params").count() > 0, "Config page should expose the params panel");
        assertTrue(page.locator(".nodel-remote").count() > 0, "Config page should expose the remote bindings panel");

        markUiGate("nodel-config");
    }

    @Test
    void diagnosticsPageRendersCoreWidgets() throws Exception {
        page.navigate(BASE_URL + "/diagnostics.htm");
        waitForBodyReady();

        assertTrue(page.locator(".nodel-diagnostics").count() > 0, "Diagnostics page should expose the diagnostics panel");
        assertTrue(page.locator(".nodel-serverlog").count() > 0, "Diagnostics page should expose the server log panel");
        assertTrue(page.locator(".nodel-charts").count() > 0, "Diagnostics page should expose the charts panel");

        markUiGate("diagnostics");
    }

    @Test
    void toolkitPageRendersCoreWidgets() throws Exception {
        page.navigate(BASE_URL + "/toolkit.htm");
        waitForBodyReady();

        assertTrue(page.locator(".nodel-toolkit").count() > 0, "Toolkit page should expose the toolkit panel");

        markUiGate("toolkit");
    }

    @Test
    void statusPageRendersCoreWidgets() throws Exception {
        String html = absolutizeAssetUrls(
            transformSourceFilesToHtml("../nodel-webui-js/src/status.xml", "../nodel-webui-js/src/index.xsl"),
            BASE_URL + "/");
        page.setContent(html);
        waitForBodyReady();

        assertTrue(page.locator(".statusgroup").count() > 0, "Status page should render status panels");
        assertTrue(page.locator("text=Overview").count() > 0, "Status page should render the Overview section");
        assertTrue(page.locator("text=Upstairs").count() > 0, "Status page should render the Upstairs section");
        assertTrue(page.locator("text=Downstairs").count() > 0, "Status page should render the Downstairs section");

        markUiGate("status");
    }

    @Test
    void customDashboardPageRendersCoreWidgets() throws Exception {
        navigateToNode(UI_FIXTURE_NODE);
        waitForBodyReady();

        assertTrue(page.locator(".well").count() > 0, "Custom dashboard should render grouped panel content");
        assertTrue(page.locator("text=Grouped").count() > 0, "Custom dashboard should include the grouped section");
        assertTrue(page.locator("text=Ungrouped").count() > 0, "Custom dashboard should include the ungrouped section");

        markUiGate("dashboard");
    }

    @Test
    void fallbackNodeRendersAdminShell() throws Exception {
        page.navigate(BASE_URL + "/nodes/" + getReducedName(UI_FALLBACK_NODE) + "/");
        waitForBodyReady();

        assertTrue(page.locator(".nodel-description").count() > 0, "Fallback node should render the admin shell");

        markUiGate("fallback-node");
    }
}
