'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const { JSDOM, VirtualConsole } = require('jsdom');

const sourceDir = path.join(__dirname, '..', 'src');
const loaderSource = fs.readFileSync(path.join(sourceDir, 'legacy-loader.js'), 'utf8');
const routerSource = fs.readFileSync(path.join(sourceDir, 'legacy-router.js'), 'utf8');

async function render(xml, options = {}) {
  const dom = new JSDOM('<!doctype html><html><head></head><body></body></html>', {
    url: options.url || 'http://nodel.test/nodes/example/index.htm',
    runScripts: 'outside-only',
    virtualConsole: new VirtualConsole()
  });
  const { window } = dom;
  const { document } = window;
  const listeners = [];
  const addListener = document.addEventListener.bind(document);
  const removeListener = document.removeEventListener.bind(document);
  const open = document.open.bind(document);
  const close = document.close.bind(document);
  let finish;
  const completed = new Promise(resolve => { finish = resolve; });

  document.addEventListener = function(type, listener, options) {
    listeners.push({ type, listener, options });
    return addListener(type, listener, options);
  };
  document.open = function() {
    // Browsers erase document listeners here. jsdom currently preserves them,
    // which otherwise masks a router installed only on the bootstrap document.
    for (const entry of listeners.splice(0)) {
      removeListener(entry.type, entry.listener, entry.options);
    }
    return open();
  };
  document.close = function() {
    const result = close();
    queueMicrotask(finish);
    return result;
  };
  window.NODEL_LEGACY_TARGET = 'fixture.xml';
  window.NODEL_LEGACY_REVEAL_GATED = false;
  window.fetch = async function(url) {
    const text = url === 'fixture.xml' ? xml :
      '<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"/>';
    return { ok: true, status: 200, text: async () => text };
  };
  if (options.router) window.eval(routerSource);
  window.eval(loaderSource);
  await completed;
  assert.notEqual(document.title, 'Legacy UI error', document.body.textContent);
  return { dom, window, document, listeners };
}

function page(content) {
  return '<pages><page title="Main"><row><column>' + content +
    '</column></row></page></pages>';
}

function routeClick(href, options = {}) {
  const location = new URL(options.pageUrl || 'http://nodel.test/nodes/example/index.htm');
  const listeners = new Set();
  const window = { location };
  const document = {
    addEventListener(type, listener) { if (type === 'click') listeners.add(listener); },
    removeEventListener(type, listener) { if (type === 'click') listeners.delete(listener); }
  };
  vm.runInNewContext(routerSource, { window, document, URL });
  const link = {
    target: options.target || '',
    hasAttribute(name) { return name === 'download' && !!options.download; },
    getAttribute(name) { return name === 'href' ? href : null; }
  };
  const event = {
    button: 0,
    defaultPrevented: false,
    target: { closest() { return link; } },
    preventDefault() { this.defaultPrevented = true; },
    ...options.event
  };
  for (const listener of listeners) listener(event);
  return { href: window.location.href, prevented: event.defaultPrevented };
}

test('preserves explicit empty action arguments and empty show values', async t => {
  const { dom, document } = await render(page(
    '<button action="Set" arg="">Clear</button>' +
    '<button action="Toggle" arg-on="" arg-off="stop">Toggle</button>' +
    '<text showevent="State" showvalue="">No state</text>'
  ));
  t.after(() => dom.window.close());
  const clear = document.querySelector('[data-action="Set"]');
  assert.equal(clear.getAttribute('data-arg'), '');
  const toggle = document.querySelector('[data-action="Toggle"]');
  assert.equal(toggle.getAttribute('data-arg-on'), '');
  assert.equal(toggle.getAttribute('data-arg-off'), 'stop');
  const state = document.querySelector('[data-showevent="State"]');
  assert.equal(state.getAttribute('data-showarg'), '');
});

test('preserves descendant text and whitespace in title, subtitle, and text', async t => {
  const { dom, document } = await render(page(
    '<title> One <b>bold</b> word </title>' +
    '<subtitle> One <b>bold</b> word </subtitle>' +
    '<text> One <b>bold</b> word </text>'
  ));
  t.after(() => dom.window.close());
  for (const selector of ['.page h4', '.page h5', '.page p']) {
    assert.equal(document.querySelector(selector).textContent, ' One bold word ');
  }
});

test('reinstalls the router after document.open clears bootstrap listeners', async t => {
  const { dom, window, document } = await render(
    '<pages><header><nodel type="nav"/></header></pages>', { router: true }
  );
  t.after(() => dom.window.close());
  const link = document.querySelector('a[href="/toolkit.xml"]');
  assert.ok(link, 'the rendered navigation should contain the legacy XML link');
  const click = new window.MouseEvent('click', { button: 0, bubbles: true, cancelable: true });
  link.dispatchEvent(click);
  assert.equal(click.defaultPrevented, true, 'the rebuilt page must still route XML links');
});

test('router installation is idempotent and works again after listeners are erased', () => {
  const listeners = new Set();
  const window = { location: new URL('http://nodel.test/index.htm') };
  const document = {
    addEventListener(type, listener, capture) {
      assert.equal(type, 'click');
      assert.equal(capture, true);
      listeners.add(listener);
    },
    removeEventListener(type, listener, capture) {
      assert.equal(type, 'click');
      assert.equal(capture, true);
      listeners.delete(listener);
    }
  };
  vm.runInNewContext(routerSource, { window, document, URL });
  assert.equal(typeof window.NODEL_LEGACY_INSTALL_ROUTER, 'function');
  window.NODEL_LEGACY_INSTALL_ROUTER();
  window.NODEL_LEGACY_INSTALL_ROUTER();
  assert.equal(listeners.size, 1, 'repeated installs must retain a single handler');
  listeners.clear();
  window.NODEL_LEGACY_INSTALL_ROUTER();
  assert.equal(listeners.size, 1, 'install must work after document.open clears handlers');
});

test('routes built-in XML links while preserving query and fragment', () => {
  assert.deepEqual(routeClick('/toolkit.xml?legacyDebug=1#Editor'), {
    href: 'http://nodel.test/toolkit.htm?legacyDebug=1#Editor', prevented: true
  });
});

test('leaves external, modified, download, and targeted link clicks alone', () => {
  const examples = [
    ['https://elsewhere.test/toolkit.xml', {}],
    ['/toolkit.xml', { event: { ctrlKey: true } }],
    ['/toolkit.xml', { event: { metaKey: true } }],
    ['/toolkit.xml', { event: { shiftKey: true } }],
    ['/toolkit.xml', { event: { altKey: true } }],
    ['/toolkit.xml', { event: { button: 1 } }],
    ['/toolkit.xml', { event: { defaultPrevented: true } }],
    ['/toolkit.xml', { download: true }],
    ['/toolkit.xml', { target: '_blank' }]
  ];
  for (const [href, options] of examples) {
    const result = routeClick(href, options);
    assert.equal(result.href, 'http://nodel.test/nodes/example/index.htm');
    assert.equal(result.prevented, !!(options.event && options.event.defaultPrevented));
  }
});
