package org.nodel;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Assumptions;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Properties;

/**
 * Shared helpers for the legacy UI extension tests.
 */
public abstract class LegacyUiTestSupport extends TestBase {

    protected static final String LEGACY_UI_TAG = "legacy-ui";
    protected static final String LEGACY_UI_PARITY_TAG = "legacy-ui-parity";
    protected static final String LEGACY_UI_FUNCTIONAL_TAG = "legacy-ui-functional";
    protected static final String LEGACY_UI_SCREENSHOT_TAG = "legacy-ui-screenshot";

    protected static final String UI_FIXTURE_NODE = "Legacy UI Extension Node";
    protected static final String UI_FALLBACK_NODE = "Legacy UI Fallback Node";

    protected static final String UI_INDEX_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<?xml-stylesheet type=\"text/xsl\" href=\"v1/index.xsl\"?>\n" +
        "<pages title='Frontend Spacing Regression' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='v1/index.xsd'>\n" +
        "  <page title='Repro'>\n" +
        "    <row>\n" +
        "      <column sm='4'>\n" +
        "        <title>Grouped</title>\n" +
        "        <group>\n" +
        "          <subtitle>Speakers</subtitle>\n" +
        "          <button join='DSP Speaker1-On' arg-on='true' arg-off='false'>Speaker 1</button>\n" +
        "          <range join='DSP Speaker1-Fader' min='-50' max='0'/>\n" +
        "        </group>\n" +
        "      </column>\n" +
        "      <column sm='4'>\n" +
        "        <title>Ungrouped</title>\n" +
        "        <subtitle>Speakers</subtitle>\n" +
        "        <button join='DSP Speaker1-On' arg-on='true' arg-off='false'>Speaker 1</button>\n" +
        "        <range join='DSP Speaker1-Fader' min='-50' max='0'/>\n" +
        "      </column>\n" +
        "    </row>\n" +
        "  </page>\n" +
        "</pages>\n";

    protected static final String BASELINE_RESOURCE = "/legacy-ui-baselines.properties";

    protected static void createLegacyUiFixtures() {
        createUiFixtureNode(UI_FIXTURE_NODE);
        uploadUiIndexFixture(UI_FIXTURE_NODE);

        createUiFixtureNode(UI_FALLBACK_NODE);
    }

    protected static void createUiFixtureNode(String nodeName) {
        try {
            Path nodesDir = resolveUiFixtureNodesDir();
            Files.createDirectories(nodesDir);

            Path nodeDir = nodesDir.resolve(nodeName);
            Files.createDirectories(nodeDir);
            Files.writeString(nodeDir.resolve("script.py"), SIMPLE_TEST_SCRIPT, StandardCharsets.UTF_8);

            Assumptions.assumeTrue(waitForNodeInList(nodeName, 30000), "Legacy UI fixture node must appear in the node list");
            Assumptions.assumeTrue(waitForNodeResponsive(nodeName, 30000), "Legacy UI fixture node must become responsive");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create legacy UI fixture node: " + nodeName, e);
        }
    }

    protected static Path resolveUiFixtureNodesDir() {
        Path nodesDir = Paths.get("nodelhost-temp/nodes");
        if (!Files.exists(nodesDir)) {
            nodesDir = Paths.get("nodel-jyhost/nodelhost-temp/nodes");
        }
        return nodesDir;
    }

    protected static Path uiFixtureIndexXmlPath(String nodeName) {
        return resolveUiFixtureNodesDir().resolve(nodeName).resolve("content").resolve("index.xml");
    }

    protected static void cleanupLegacyUiFixtures() {
        deleteTestNode(UI_FIXTURE_NODE);
        deleteTestNode(UI_FALLBACK_NODE);
    }

    protected static void uploadUiIndexFixture(String nodeName) {
        APIResponse response = page.request().post(
            REST_BASE + "/nodes/" + encode(nodeName) + "/files/save?path=content/index.xml",
            RequestOptions.create()
                .setHeader("Content-Type", "application/octet-stream")
                .setData(UI_INDEX_XML));
        Assumptions.assumeTrue(response.status() == 200, "content/index.xml upload should succeed");
    }

    protected static void installRestBlocker() {
        page.addInitScript("(() => {" +
            "const shouldBlock = (url) => typeof url === 'string' && url.indexOf('/REST/') !== -1;" +
            "const originalFetch = window.fetch;" +
            "window.fetch = function(input, init) {" +
            "  const url = typeof input === 'string' ? input : (input && input.url) ? input.url : '';" +
            "  if (shouldBlock(url)) return Promise.reject(new Error('REST blocked'));" +
            "  return originalFetch.apply(this, arguments);" +
            "};" +
            "const originalOpen = XMLHttpRequest.prototype.open;" +
            "const originalSend = XMLHttpRequest.prototype.send;" +
            "XMLHttpRequest.prototype.open = function(method, url) {" +
            "  this.__nodelBlockRest = shouldBlock(url);" +
            "  return originalOpen.apply(this, arguments);" +
            "};" +
            "XMLHttpRequest.prototype.send = function() {" +
            "  if (this.__nodelBlockRest) { try { this.abort(); } catch (e) {} return; }" +
            "  return originalSend.apply(this, arguments);" +
            "};" +
            "})();");
    }

    protected static String fetchText(String path) {
        APIResponse response = page.request().get(path);
        Assumptions.assumeTrue(response.status() == 200, "GET " + path + " should return 200");
        return response.text();
    }

    protected static String transformXmlToHtml(String xmlUrl, String xslUrl) throws Exception {
        StreamSource xslSource = new StreamSource(new StringReader(fetchText(xslUrl)));
        xslSource.setSystemId(xslUrl);

        StreamSource xmlSource = new StreamSource(new StringReader(fetchText(xmlUrl)));
        xmlSource.setSystemId(xmlUrl);

        Transformer transformer = TransformerFactory.newInstance().newTransformer(xslSource);
        StringWriter output = new StringWriter();
        transformer.transform(xmlSource, new StreamResult(output));
        return output.toString();
    }

    protected static String transformSourceFilesToHtml(String xmlRelativePath, String xslRelativePath) throws Exception {
        Path xmlPath = Paths.get(xmlRelativePath);
        Path xslPath = Paths.get(xslRelativePath);

        StreamSource xslSource = new StreamSource(new StringReader(Files.readString(xslPath, StandardCharsets.UTF_8)));
        xslSource.setSystemId(xslPath.toAbsolutePath().toUri().toString());

        StreamSource xmlSource = new StreamSource(new StringReader(Files.readString(xmlPath, StandardCharsets.UTF_8)));
        xmlSource.setSystemId(xmlPath.toAbsolutePath().toUri().toString());

        Transformer transformer = TransformerFactory.newInstance().newTransformer(xslSource);
        StringWriter output = new StringWriter();
        transformer.transform(xmlSource, new StreamResult(output));
        return output.toString();
    }

    protected static String absolutizeAssetUrls(String html, String baseUrl) {
        return html
            .replace("href=\"v1/", "href=\"" + baseUrl + "v1/")
            .replace("src=\"v1/", "src=\"" + baseUrl + "v1/")
            .replace("href='v1/", "href='" + baseUrl + "v1/")
            .replace("src='v1/", "src='" + baseUrl + "v1/");
    }

    protected static String normalizedBodyMarkup() {
        return page.evaluate("() => {" +
            "  const clone = document.body.cloneNode(true);" +
            "  clone.querySelectorAll('script, style, #clock').forEach((el) => el.remove());" +
            "  return clone.innerHTML.replace(/\\s+/g, ' ').trim();" +
            "}").toString();
    }

    protected static void assertSameRenderedMarkup(String expectedHtml, String actualHtml) {
        String expected = expectedHtml.replaceAll("\\s+", " ").trim();
        String actual = actualHtml.replaceAll("\\s+", " ").trim();
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    protected static String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    protected static Path uiGatePath(String gateName) {
        return Paths.get("build", "legacy-ui-gates", gateName + ".pass");
    }

    protected static void markUiGate(String gateName) throws Exception {
        Path path = uiGatePath(gateName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "pass\n", StandardCharsets.UTF_8);
    }

    protected static boolean uiGateExists(String gateName) {
        return Files.exists(uiGatePath(gateName));
    }

    protected static Path uiScreenshotPath(String screenshotName) {
        return Paths.get("build", "legacy-ui-screenshots", screenshotName + ".png");
    }

    protected static Path generatedBaselinePath() {
        return Paths.get("build", "legacy-ui-baselines.generated.properties");
    }

    protected static boolean updatingUiBaselines() {
        return System.getenv("UPDATE_UI_BASELINES") != null;
    }

    protected static Properties loadUiBaselines() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = LegacyUiTestSupport.class.getResourceAsStream(BASELINE_RESOURCE)) {
            if (input != null) {
                properties.load(input);
            }
        }
        return properties;
    }

    protected static void writeGeneratedBaseline(String screenshotName, String hash) throws IOException {
        Properties properties = new Properties();
        Path generated = generatedBaselinePath();
        if (Files.exists(generated)) {
            try (InputStream input = Files.newInputStream(generated)) {
                properties.load(input);
            }
        }
        properties.setProperty(screenshotName, hash);
        Files.createDirectories(generated.getParent());
        try (java.io.OutputStream output = Files.newOutputStream(generated)) {
            properties.store(output, "Generated legacy UI screenshot hashes");
        }
    }

    protected static byte[] captureBodyScreenshot(String screenshotName) throws Exception {
        Path path = uiScreenshotPath(screenshotName);
        Files.createDirectories(path.getParent());
        page.evaluate("() => { const clock = document.querySelector('#clock'); if (clock) clock.remove(); }");
        byte[] data = page.screenshot();
        Files.write(path, data);
        return data;
    }

    protected static void waitForBodyReady() {
        waitForElement(".navbar");
    }

    protected static void showOnlyPageSection(String sectionTitle) {
        page.evaluate("(sectionTitle) => {" +
            "  document.body.classList.add('nodel-legacy-ready');" +
            "  document.querySelectorAll('.page').forEach((el) => {" +
            "    el.style.display = (el.dataset.section === sectionTitle) ? 'block' : 'none';" +
            "  });" +
            "}", sectionTitle);
    }

    protected static String stripScripts(String html) {
        return html.replaceAll("(?is)<script\\b[^>]*>.*?</script>", "");
    }

    protected static void assumeUiGate(String gateName) {
        Assumptions.assumeTrue(uiGateExists(gateName), "Functional UI gate not set for " + gateName);
    }
}
