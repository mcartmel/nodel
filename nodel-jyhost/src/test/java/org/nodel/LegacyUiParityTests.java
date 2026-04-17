package org.nodel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(LegacyUiTestSupport.LEGACY_UI_TAG)
@Tag(LegacyUiTestSupport.LEGACY_UI_PARITY_TAG)
public class LegacyUiParityTests extends LegacyUiTestSupport {

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
    void localsXmlMatchesLegacyLoader() throws Exception {
        assertPageMatchesLoader("/locals.xml", "/locals.htm", BASE_URL + "/");
    }

    @Test
    void nodesXmlMatchesLegacyLoader() throws Exception {
        assertPageMatchesLoader("/nodes.xml", "/nodes.htm", BASE_URL + "/nodes.htm");
    }

    @Test
    void diagnosticsXmlMatchesLegacyLoader() throws Exception {
        assertPageMatchesLoader("/diagnostics.xml", "/diagnostics.htm", BASE_URL + "/diagnostics.htm");
    }

    @Test
    void toolkitXmlMatchesLegacyLoader() throws Exception {
        assertPageMatchesLoader("/toolkit.xml", "/toolkit.htm", BASE_URL + "/toolkit.htm");
    }

    @Test
    void statusXmlMatchesLegacyLoader() throws Exception {
        assertPageMatchesLoader("/status.xml", "/status.htm#Overview", BASE_URL + "/status.htm");
    }

    @Test
    void fallbackNodeXmlMatchesLegacyLoader() throws Exception {
        String reduced = getReducedName(UI_FALLBACK_NODE);
        assertPageMatchesLoader(
            "/nodes/" + reduced + "/nodel.xml",
            "/nodes/" + reduced + "/",
            BASE_URL + "/nodes/" + reduced + "/");
    }

    @Test
    void customDashboardXmlMatchesLegacyLoader() throws Exception {
        String reduced = getReducedName(UI_FIXTURE_NODE);
        assertPageMatchesLoader(
            "/nodes/" + reduced + "/index.xml",
            "/nodes/" + reduced + "/",
            BASE_URL + "/nodes/" + reduced + "/");
    }

    private void assertPageMatchesLoader(String xmlPath, String loaderPath, String loaderUrl) throws Exception {
        String xmlUrl = BASE_URL + xmlPath;
        String xslUrl = xmlUrl.replaceFirst("/[^/]+$", "/v1/index.xsl");
        String transformedHtml = absolutizeAssetUrls(transformXmlToHtml(xmlUrl, xslUrl), loaderUrl);

        page.setContent(transformedHtml);
        waitForBodyReady();
        assertSelectorsPresent(xmlPath, "Transformed markup");

        page.navigate(BASE_URL + loaderPath);
        waitForBodyReady();
        assertTrue(normalizedBodyMarkup().length() > 0, "Loader markup should not be empty for " + loaderPath);
        assertSelectorsPresent(xmlPath, "Loader markup");
    }

    private void assertSelectorsPresent(String xmlPath, String label) {
        for (String selector : expectedSelectors(xmlPath)) {
            assertTrue(hasSelector(selector), label + " should include " + selector + " for " + xmlPath);
        }
    }

    private String[] expectedSelectors(String xmlPath) {
        if (xmlPath.endsWith("locals.xml")) {
            return new String[]{".nodel-add", ".nodel-locals"};
        }
        if (xmlPath.endsWith("nodes.xml")) {
            return new String[]{".nodel-list"};
        }
        if (xmlPath.endsWith("nodel.xml")) {
            return new String[]{".nodel-description", ".nodel-console", ".nodel-editor", ".nodel-actsig", ".nodel-log", ".nodel-params", ".nodel-remote"};
        }
        if (xmlPath.endsWith("diagnostics.xml")) {
            return new String[]{".nodel-diagnostics", ".nodel-serverlog", ".nodel-charts"};
        }
        if (xmlPath.endsWith("toolkit.xml")) {
            return new String[]{".nodel-toolkit"};
        }
        if (xmlPath.endsWith("status.xml")) {
            return new String[]{".statusgroup"};
        }
        if (xmlPath.endsWith("index.xml")) {
            return new String[]{".well"};
        }
        return new String[0];
    }

    private boolean hasSelector(String selector) {
        return page.locator(selector).count() > 0;
    }
}
