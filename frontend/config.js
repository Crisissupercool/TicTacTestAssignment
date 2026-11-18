// Konfiguration für die lokale Entwicklung (siehe docs/webkonsole.md).
// In der Cloud schreibt OpenTofu eine eigene config.js in den Bucket. Diese Datei gehört
// deshalb NICHT ins Release-Archiv und wird beim Deployment weder überschrieben noch gelöscht.
window.TICTACTEST_CONFIG = {
  consoleUrl: 'http://localhost:7681',
  versionUrl: 'http://localhost:8081/version.txt',
};
