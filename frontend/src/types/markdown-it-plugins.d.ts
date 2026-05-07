declare module "markdown-it-abbr" {
  import type { PluginSimple } from "markdown-it";
  const plugin: PluginSimple;
  export default plugin;
}

declare module "markdown-it-container" {
  import type MarkdownIt from "markdown-it";

  type ContainerPluginOptions = {
    marker?: string;
    validate?: (params: string, markup: string) => boolean;
    render?: (
      tokens: unknown[],
      index: number,
      options: MarkdownIt.Options,
      env: unknown,
      self: MarkdownIt.Renderer
    ) => string;
  };

  function plugin(md: MarkdownIt, name: string, options?: ContainerPluginOptions): void;
  export default plugin;
}

declare module "markdown-it-deflist" {
  import type { PluginSimple } from "markdown-it";
  const plugin: PluginSimple;
  export default plugin;
}

declare module "markdown-it-emoji" {
  import type { PluginSimple } from "markdown-it";
  export const bare: PluginSimple;
  export const light: PluginSimple;
  export const full: PluginSimple;
}

declare module "markdown-it-footnote" {
  import type { PluginSimple } from "markdown-it";
  const plugin: PluginSimple;
  export default plugin;
}

declare module "markdown-it-ins" {
  import type { PluginSimple } from "markdown-it";
  const plugin: PluginSimple;
  export default plugin;
}

declare module "markdown-it-mark" {
  import type { PluginSimple } from "markdown-it";
  const plugin: PluginSimple;
  export default plugin;
}

declare module "markdown-it-sub" {
  import type { PluginSimple } from "markdown-it";
  const plugin: PluginSimple;
  export default plugin;
}

declare module "markdown-it-sup" {
  import type { PluginSimple } from "markdown-it";
  const plugin: PluginSimple;
  export default plugin;
}
