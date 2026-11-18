const config = window.TICTACTEST_CONFIG || {};

document.getElementById('console').src = config.consoleUrl;

async function showVersion(elementId, url) {
  const element = document.getElementById(elementId);
  try {
    const response = await fetch(url, { cache: 'no-store' });
    if (!response.ok) {
      throw new Error('HTTP ' + response.status);
    }
    element.textContent = (await response.text()).trim();
  } catch (error) {
    element.textContent = 'unbekannt';
    console.error('Version von ' + url + ' nicht lesbar:', error);
  }
}

showVersion('frontend-version', 'version.txt');
showVersion('backend-version', config.versionUrl);
