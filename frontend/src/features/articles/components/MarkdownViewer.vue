<script setup lang="ts">
import { computed } from "vue";
import DOMPurify from "dompurify";
import hljs from "highlight.js/lib/common";
import MarkdownIt from "markdown-it";
import markdownItAbbr from "markdown-it-abbr";
import markdownItContainer from "markdown-it-container";
import markdownItDeflist from "markdown-it-deflist";
import { full as markdownItEmoji } from "markdown-it-emoji";
import markdownItFootnote from "markdown-it-footnote";
import markdownItIns from "markdown-it-ins";
import markdownItMark from "markdown-it-mark";
import markdownItSub from "markdown-it-sub";
import markdownItSup from "markdown-it-sup";

interface CodeBlockMeta {
  language: string;
  fileName: string;
  showLineNumbers: boolean;
}

const props = defineProps<{
  source: string;
}>();

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: true,
})
  .use(markdownItAbbr)
  .use(markdownItContainer, "warning")
  .use(markdownItDeflist)
  .use(markdownItEmoji)
  .use(markdownItFootnote)
  .use(markdownItIns)
  .use(markdownItMark)
  .use(markdownItSub)
  .use(markdownItSup);

markdown.renderer.rules.fence = (tokens, index) => {
  const token = tokens[index];
  return renderCodeBlock(token.content, parseCodeBlockMeta(token.info));
};

const renderedHtml = computed(() => DOMPurify.sanitize(markdown.render(props.source || "")));

function parseCodeBlockMeta(info = ""): CodeBlockMeta {
  const [language = "", ...metaParts] = info.trim().split(/\s+/);
  const meta = metaParts.join(" ");

  return {
    language: language.trim(),
    fileName: parseFileName(meta),
    showLineNumbers: /\b(showLineNumbers|lineNumbers|linenums)\b/i.test(meta),
  };
}

function parseFileName(meta: string): string {
  const quoted = meta.match(/\b(?:filename|file|title)=["']([^"']+)["']/i);
  if (quoted?.[1]) return quoted[1];

  const bare = meta.match(/\b(?:filename|file|title)=([^\s}]+)/i);
  return bare?.[1] || "";
}

function renderCodeBlock(code: string, meta: CodeBlockMeta): string {
  const language = resolveLanguage(meta.language);
  const fileName = markdown.utils.escapeHtml(meta.fileName);
  const languageLabel = markdown.utils.escapeHtml(meta.language || "text");
  const lines = code.replace(/\n$/, "").split("\n");
  const lineMarkup = lines
    .map((line, index) => {
      const lineNumber = index + 1;
      const highlightedLine = highlightCodeLine(line, language);
      const number = meta.showLineNumbers
        ? `<span class="markdown-code-line-number" aria-hidden="true">${lineNumber}</span>`
        : "";

      return `<span class="markdown-code-line">${number}<span class="markdown-code-line-content">${highlightedLine || " "}</span></span>`;
    })
    .join("");
  const header = fileName
    ? `<div class="markdown-code-header"><span class="markdown-code-file">${fileName}</span><span class="markdown-code-language">${languageLabel}</span></div>`
    : "";
  const lineNumberClass = meta.showLineNumbers ? " has-line-numbers" : "";

  return `<div class="markdown-code-block${lineNumberClass}">${header}<pre><code class="hljs language-${languageLabel}">${lineMarkup}</code></pre></div>`;
}

function resolveLanguage(language: string): string {
  if (!language) return "";
  return hljs.getLanguage(language) ? language : "";
}

function highlightCodeLine(line: string, language: string): string {
  if (!language) return markdown.utils.escapeHtml(line);
  return hljs.highlight(line, { language, ignoreIllegals: true }).value;
}
</script>

<template>
  <div class="markdown-viewer" v-html="renderedHtml" />
</template>
