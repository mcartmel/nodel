# Legacy wrapper checks

Run these checks from `nodel-webui-js` with Node.js 24 (jsdom also supports
Node.js 20.19+ or 22.13+) and a JDK with `jshell` on `PATH`:

```sh
npm ci --legacy-peer-deps --ignore-scripts
npm run test:legacy-wrapper
npm run compare:markup
```

The regression tests cover wrapper routing, restoration of the click handler
after document replacement, empty action/visibility values, and descendant
text content. They use a simulated document lifecycle; real-browser navigation
and live Nodel runtime testing are still needed.

The markup comparison renders seven built-in XML pages and two compatibility
fixtures with both the Java XSLT engine and the JavaScript wrapper. It compares
normalized HTML without executing the legacy runtime. The Java XSLT engine is
only a test oracle; the wrapper does not execute XSLT in the browser.

Known limitation: the router currently changes same-origin `.xml` links to
`.htm`. Only the built-in wrapper files are shipped. Custom XML pages can be
opened through `index.htm?xml=filename.xml`, but arbitrary custom XML links need
corresponding wrappers or a routing follow-up. Nested-directory asset and REST
base paths need validation before generalizing that routing.
