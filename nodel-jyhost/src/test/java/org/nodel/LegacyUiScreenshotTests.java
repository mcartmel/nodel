package org.nodel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(LegacyUiTestSupport.LEGACY_UI_TAG)
@Tag(LegacyUiTestSupport.LEGACY_UI_SCREENSHOT_TAG)
public class LegacyUiScreenshotTests extends LegacyUiTestSupport {

    @BeforeAll
    static void setup() {
        initBrowser();
        createLegacyUiFixtures();
        installRestBlocker();
    }

    @AfterAll
    static void teardown() {
        cleanupLegacyUiFixtures();
        closeBrowser();
    }

    @Test
    void localsPageScreenshot() throws Exception {
        page.setContent(absolutizeAssetUrls(
            stripScripts(transformSourceFilesToHtml("../nodel-webui-js/src/locals.xml", "../nodel-webui-js/src/index.xsl")),
            BASE_URL + "/"));
        showOnlyPageSection("Locals");
        waitForBodyReady();
        assertScreenshot("locals", null);
    }

    @Test
    void nodesPageScreenshot() throws Exception {
        page.setContent(absolutizeAssetUrls(
            stripScripts(transformSourceFilesToHtml("../nodel-webui-js/src/nodes.xml", "../nodel-webui-js/src/index.xsl")),
            BASE_URL + "/"));
        showOnlyPageSection("List");
        waitForBodyReady();
        assertScreenshot("nodes", null);
    }

    @Test
    void nodelActivityPageScreenshot() throws Exception {
        page.setContent(absolutizeAssetUrls(
            stripScripts(transformSourceFilesToHtml("../nodel-webui-js/src/nodel.xml", "../nodel-webui-js/src/index.xsl")),
            BASE_URL + "/"));
        showOnlyPageSection("Activity");
        waitForBodyReady();
        assertScreenshot("nodel-activity", null);
    }

    @Test
    void nodelConfigPageScreenshot() throws Exception {
        page.setContent(absolutizeAssetUrls(
            stripScripts(transformSourceFilesToHtml("../nodel-webui-js/src/nodel.xml", "../nodel-webui-js/src/index.xsl")),
            BASE_URL + "/"));
        showOnlyPageSection("Config");
        waitForBodyReady();
        assertScreenshot("nodel-config", null);
    }

    @Test
    void diagnosticsPageScreenshot() throws Exception {
        String html = absolutizeAssetUrls(
            stripScripts(transformSourceFilesToHtml("../nodel-webui-js/src/diagnostics.xml", "../nodel-webui-js/src/index.xsl")),
            BASE_URL + "/");
        page.setContent(html);
        showOnlyPageSection("Diagnostics");
        waitForBodyReady();

        byte[] screenshot = captureBodyScreenshot("diagnostics");
        String hash = sha256(screenshot);
        if (updatingUiBaselines()) {
            writeGeneratedBaseline("diagnostics", hash);
            return;
        }

        Properties baselines = loadUiBaselines();
        assertEquals(baselines.getProperty("diagnostics"), hash, "Screenshot hash should match baseline for diagnostics");
    }

    @Test
    void toolkitPageScreenshot() throws Exception {
        String html = absolutizeAssetUrls(
            stripScripts(transformSourceFilesToHtml("../nodel-webui-js/src/toolkit.xml", "../nodel-webui-js/src/index.xsl")),
            BASE_URL + "/");
        page.setContent(html);
        showOnlyPageSection("Toolkit");
        waitForBodyReady();

        byte[] screenshot = captureBodyScreenshot("toolkit");
        String hash = sha256(screenshot);
        if (updatingUiBaselines()) {
            writeGeneratedBaseline("toolkit", hash);
            return;
        }

        Properties baselines = loadUiBaselines();
        assertEquals(baselines.getProperty("toolkit"), hash, "Screenshot hash should match baseline for toolkit");
    }

    @Test
    void statusPageScreenshot() throws Exception {
        String html = absolutizeAssetUrls(
            stripScripts(transformSourceFilesToHtml("../nodel-webui-js/src/status.xml", "../nodel-webui-js/src/index.xsl")),
            BASE_URL + "/");
        page.setContent(html);
        showOnlyPageSection("Overview");
        waitForBodyReady();

        byte[] screenshot = captureBodyScreenshot("status");
        String hash = sha256(screenshot);
        if (updatingUiBaselines()) {
            writeGeneratedBaseline("status", hash);
            return;
        }

        Properties baselines = loadUiBaselines();
        assertEquals(baselines.getProperty("status"), hash, "Screenshot hash should match baseline for status");
    }

    @Test
    void dashboardPageScreenshot() throws Exception {
        page.setContent(absolutizeAssetUrls(
            stripScripts(transformSourceFilesToHtml(uiFixtureIndexXmlPath(UI_FIXTURE_NODE).toString(), "../nodel-webui-js/src/index.xsl")),
            BASE_URL + "/nodes/" + getReducedName(UI_FIXTURE_NODE) + "/"));
        showOnlyPageSection("Repro");
        waitForBodyReady();
        assertScreenshot("dashboard", null);
    }

    private void assertScreenshot(String screenshotName, String url) throws Exception {
        assumeUiGate(screenshotName);

        if (url != null) {
            page.navigate(url);
            waitForBodyReady();
        }

        byte[] screenshot = captureBodyScreenshot(screenshotName);
        String hash = sha256(screenshot);

        if (updatingUiBaselines()) {
            writeGeneratedBaseline(screenshotName, hash);
            return;
        }

        Properties baselines = loadUiBaselines();
        String expected = baselines.getProperty(screenshotName);
        assertEquals(expected, hash, "Screenshot hash should match baseline for " + screenshotName);
    }
}
