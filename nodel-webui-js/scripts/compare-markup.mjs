#!/usr/bin/env node
import fs from 'node:fs/promises';
import path from 'node:path';

const RAW_TEXT_TAGS = new Set(['script', 'style']);
const VOID_TAGS = new Set([
  'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link',
  'meta', 'param', 'source', 'track', 'wbr'
]);
const GENERATED_ATTRS = new Set(['id', 'for', 'aria-labelledby', 'data-target']);
const GENERATED_PREFIXES = [
  'addgrp_',
  'editgrp_',
  'nodenamval_',
  'templateval_',
  'scriptnameval_',
  'legacy_'
];

function usage() {
  console.error('Usage: compare-markup <expected.html> <actual.html>');
  console.error('   or: compare-markup --expected-dir <dir> --actual-dir <dir>');
  process.exit(2);
}

function stripBom(text) {
  return text.replace(/^\uFEFF/, '');
}

function isWhitespaceText(text) {
  return /^\s*$/.test(text);
}

function normalizeGeneratedValue(value) {
  var prefix = '';
  var isHash = false;
  if (value.startsWith('#')) {
    isHash = true;
    value = value.slice(1);
  }
  for (var i = 0; i < GENERATED_PREFIXES.length; i++) {
    var candidate = GENERATED_PREFIXES[i];
    if (value.indexOf(candidate) === 0) {
      prefix = candidate;
      return (isHash ? '#' : '') + prefix + 'GEN';
    }
  }
  return (isHash ? '#' : '') + value;
}

function parseAttrs(input) {
  var attrs = [];
  var re = /([:^A-Za-z0-9_\-\.]+|[A-Za-z_][A-Za-z0-9_:\-\.]*)\s*(?:=\s*("[^"]*"|'[^']*'|[^\s"'`=<>]+))?/g;
  var match;
  while ((match = re.exec(input))) {
    var name = match[1];
    var value = '';
    if (match[2]) {
      value = match[2];
      if ((value[0] === '"' && value[value.length - 1] === '"') || (value[0] === '\'' && value[value.length - 1] === '\'')) {
        value = value.slice(1, -1);
      }
    }
    attrs.push([name, value]);
  }
  return attrs;
}

function parseMarkup(input) {
  var root = { type: 'root', children: [] };
  var stack = [root];
  var i = 0;
  input = stripBom(input);

  while (i < input.length) {
    if (input[i] !== '<') {
      var next = input.indexOf('<', i);
      if (next < 0) next = input.length;
      var text = input.slice(i, next);
      if (!isWhitespaceText(text)) {
        stack[stack.length - 1].children.push({ type: 'text', text: text });
      }
      i = next;
      continue;
    }

    if (input.startsWith('<!--', i)) {
      var commentEnd = input.indexOf('-->', i + 4);
      if (commentEnd < 0) throw new Error('Unclosed comment');
      i = commentEnd + 3;
      continue;
    }
    if (input.startsWith('<?', i)) {
      var piEnd = input.indexOf('?>', i + 2);
      if (piEnd < 0) throw new Error('Unclosed processing instruction');
      i = piEnd + 2;
      continue;
    }
    if (input.startsWith('<!', i)) {
      var declEnd = input.indexOf('>', i + 2);
      if (declEnd < 0) throw new Error('Unclosed declaration');
      i = declEnd + 1;
      continue;
    }

    if (input[i + 1] === '/') {
      var closeEnd = input.indexOf('>', i + 2);
      if (closeEnd < 0) throw new Error('Unclosed closing tag');
      var closeName = input.slice(i + 2, closeEnd).trim().toLowerCase();
      while (stack.length > 1) {
        var node = stack.pop();
        if (node.name === closeName) break;
      }
      i = closeEnd + 1;
      continue;
    }

    var openEnd = input.indexOf('>', i + 1);
    if (openEnd < 0) throw new Error('Unclosed opening tag');
    var tagContent = input.slice(i + 1, openEnd);
    var selfClosing = /\/\s*$/.test(tagContent);
    if (selfClosing) {
      tagContent = tagContent.replace(/\/\s*$/, '').trim();
    }
    var firstSpace = tagContent.search(/\s/);
    var name = (firstSpace < 0 ? tagContent : tagContent.slice(0, firstSpace)).trim().toLowerCase();
    var attrText = firstSpace < 0 ? '' : tagContent.slice(firstSpace + 1);
    var attrs = parseAttrs(attrText);
    var element = { type: 'element', name: name, attrs: attrs, children: [] };
    stack[stack.length - 1].children.push(element);

    if (RAW_TEXT_TAGS.has(name) && !selfClosing) {
      var closeSeq = '</' + name + '>';
      var lower = input.toLowerCase();
      var closeIndex = lower.indexOf(closeSeq, openEnd + 1);
      if (closeIndex < 0) throw new Error('Unclosed raw-text tag: ' + name);
      var rawText = input.slice(openEnd + 1, closeIndex);
      if (rawText.length && !isWhitespaceText(rawText)) {
        element.children.push({ type: 'text', text: rawText });
      }
      i = closeIndex + closeSeq.length;
      continue;
    }

    if (!selfClosing && !VOID_TAGS.has(name)) {
      stack.push(element);
    }
    i = openEnd + 1;
  }

  return root;
}

function canonicalize(node) {
  if (node.type === 'text') {
    return { type: 'text', text: node.text };
  }
  if (node.type === 'element') {
    var attrs = node.attrs.slice().map(function(pair) {
      var name = pair[0].toLowerCase();
      var value = pair[1];
      if (GENERATED_ATTRS.has(name)) {
        value = normalizeGeneratedValue(value);
      }
      return [name, value];
    }).sort(function(a, b) {
      return a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : (a[1] < b[1] ? -1 : a[1] > b[1] ? 1 : 0);
    });
    return {
      type: 'element',
      name: node.name,
      attrs: attrs,
      children: node.children.filter(function(child) {
        return child.type !== 'text' || !isWhitespaceText(child.text);
      }).map(canonicalize)
    };
  }
  return {
    type: 'root',
    children: node.children.filter(function(child) {
      return child.type !== 'text' || !isWhitespaceText(child.text);
    }).map(canonicalize)
  };
}

function compareNodes(a, b, pathLabel) {
  if (a.type !== b.type) return pathLabel + ': node type mismatch';
  if (a.type === 'text') {
    if (a.text !== b.text) return pathLabel + ': text mismatch';
    return null;
  }
  if (a.type === 'element') {
    if (a.name !== b.name) return pathLabel + ': tag mismatch ' + a.name + ' != ' + b.name;
    if (a.attrs.length !== b.attrs.length) return pathLabel + ': attribute count mismatch on <' + a.name + '>';
    for (var i = 0; i < a.attrs.length; i++) {
      if (a.attrs[i][0] !== b.attrs[i][0] || a.attrs[i][1] !== b.attrs[i][1]) {
        return pathLabel + ': attribute mismatch on <' + a.name + '>';
      }
    }
    if (a.children.length !== b.children.length) return pathLabel + ': child count mismatch on <' + a.name + '>';
    for (var j = 0; j < a.children.length; j++) {
      var childPath = pathLabel + '/' + a.name + '[' + j + ']';
      var diff = compareNodes(a.children[j], b.children[j], childPath);
      if (diff) return diff;
    }
    return null;
  }
  if (a.children.length !== b.children.length) return pathLabel + ': child count mismatch';
  for (var k = 0; k < a.children.length; k++) {
    var rootDiff = compareNodes(a.children[k], b.children[k], pathLabel + '/root[' + k + ']');
    if (rootDiff) return rootDiff;
  }
  return null;
}

async function readFile(filePath) {
  return stripBom(await fs.readFile(filePath, 'utf8'));
}

async function comparePair(expectedPath, actualPath) {
  var expected = canonicalize(parseMarkup(await readFile(expectedPath)));
  var actual = canonicalize(parseMarkup(await readFile(actualPath)));
  var diff = compareNodes(expected, actual, path.basename(expectedPath));
  if (diff) {
    throw new Error(diff + '\nexpected: ' + expectedPath + '\nactual:   ' + actualPath);
  }
}

async function compareDirs(expectedDir, actualDir) {
  var entries = (await fs.readdir(expectedDir)).filter(function(name) {
    return /\.(html?|xml)$/i.test(name);
  }).sort();
  for (var i = 0; i < entries.length; i++) {
    var expectedPath = path.join(expectedDir, entries[i]);
    var actualPath = path.join(actualDir, entries[i]);
    await comparePair(expectedPath, actualPath);
  }
}

async function main(argv) {
  if (argv.includes('--expected-dir') || argv.includes('--actual-dir')) {
    var expectedIndex = argv.indexOf('--expected-dir');
    var actualIndex = argv.indexOf('--actual-dir');
    if (expectedIndex < 0 || actualIndex < 0 || !argv[expectedIndex + 1] || !argv[actualIndex + 1]) usage();
    await compareDirs(argv[expectedIndex + 1], argv[actualIndex + 1]);
    return;
  }

  if (argv.length !== 2) usage();
  await comparePair(argv[0], argv[1]);
}

main(process.argv.slice(2)).catch(function(error) {
  console.error(error && error.stack ? error.stack : String(error));
  process.exit(1);
});
