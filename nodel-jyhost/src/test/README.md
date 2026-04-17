# Legacy UI Test README

Temporary overview of the legacy UI browser tests in `nodel-jyhost`.

## Shared Setup

All three suites use `LegacyUiTestSupport` and the isolated host on port `18085`.

- start a dedicated Nodel instance for the suite
- create fixture nodes in `nodelhost-temp/nodes`
- use Playwright for browser access
- clean up the fixture nodes after the suite

## `LegacyUiParityTests`

These compare XML/XSLT rendering against the live legacy loader pages.

- `localsXmlMatchesLegacyLoader()`
  - compares the rendered `locals.xml` page to `/locals.htm`
- `nodesXmlMatchesLegacyLoader()`
  - compares `nodes.xml` to `/nodes.htm`
- `diagnosticsXmlMatchesLegacyLoader()`
  - compares `diagnostics.xml` to `/diagnostics.htm`
- `toolkitXmlMatchesLegacyLoader()`
  - compares `toolkit.xml` to `/toolkit.htm`
- `statusXmlMatchesLegacyLoader()`
  - compares `status.xml` to `/status.htm`
  - currently skipped in this environment because the live route is unstable
- `fallbackNodeXmlMatchesLegacyLoader()`
  - compares the fallback node XML view to the live node shell
- `customDashboardXmlMatchesLegacyLoader()`
  - compares the custom dashboard XML view to the live node shell

## `LegacyUiFunctionalTests`

These verify that the important UI widgets appear on the rendered pages.

- `localsPageRendersCoreWidgets()`
  - checks the locals page shell and add-node UI
- `nodesPageRendersCoreWidgets()`
  - checks the node list shell
- `nodelActivityPageRendersCoreWidgets()`
  - checks the node activity page panels
- `nodelConfigPageRendersCoreWidgets()`
  - checks the config panels for a node
- `diagnosticsPageRendersCoreWidgets()`
  - checks the diagnostics panels
- `toolkitPageRendersCoreWidgets()`
  - checks the toolkit panel
- `statusPageRendersCoreWidgets()`
  - checks the status page sections
- `customDashboardPageRendersCoreWidgets()`
  - checks a custom dashboard page created by the fixture node
- `fallbackNodeRendersAdminShell()`
  - checks the fallback node still renders the admin shell

## `LegacyUiScreenshotTests`

These capture deterministic screenshots and compare their SHA-256 hashes against checked-in baselines.

- `localsPageScreenshot()`
  - screenshot of the locals page shell
- `nodesPageScreenshot()`
  - screenshot of the node list shell
- `nodelActivityPageScreenshot()`
  - screenshot of the node activity page
- `nodelConfigPageScreenshot()`
  - screenshot of the config page
- `diagnosticsPageScreenshot()`
  - screenshot of the diagnostics page
- `toolkitPageScreenshot()`
  - screenshot of the toolkit page
- `statusPageScreenshot()`
  - screenshot of the status page
- `dashboardPageScreenshot()`
  - screenshot of the custom dashboard page

## Notes

- Functional tests create gate files under `build/legacy-ui-gates/`.
- Screenshot tests only run after the functional gate for the same page has passed.
- Baseline hashes live in `nodel-jyhost/src/test/resources/legacy-ui-baselines.properties`.
