(function() {
  'use strict';

  function route(url) {
    if (url.origin !== window.location.origin) return null;
    if (!/\.xml$/i.test(url.pathname)) return null;
    var routed = new URL(url.href);
    routed.pathname = routed.pathname.replace(/\.xml$/i, '.htm');
    return routed.href;
  }

  document.addEventListener('click', function(event) {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    var link = event.target.closest && event.target.closest('a[href]');
    if (!link || link.target || link.hasAttribute('download')) return;
    var routed = route(new URL(link.getAttribute('href'), window.location.href));
    if (!routed) return;
    event.preventDefault();
    window.location.href = routed;
  }, true);
})();
