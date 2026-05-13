const releaseExtensionDownloadUrl =
  "https://github.com/t-shirayama/articleshelf/releases/latest/download/articleshelf-chrome-extension.zip";

const localExtensionDownloadUrl = "/downloads/articleshelf-chrome-extension-local.zip";

export const extensionDownloadUrl =
  import.meta.env.VITE_EXTENSION_DOWNLOAD_URL ??
  (import.meta.env.PROD ? releaseExtensionDownloadUrl : localExtensionDownloadUrl);

export const shouldLoadExtensionVersionFromGitHub =
  !import.meta.env.VITE_EXTENSION_VERSION &&
  extensionDownloadUrl === releaseExtensionDownloadUrl;
