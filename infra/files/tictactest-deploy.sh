#!/bin/bash
# Bringt die Version eines GitHub-Releases auf die Instanz. Aufruf (als root, z. B. per SSM):
#   tictactest-deploy 1.2.0
# Die Datei ist Teil der Infrastruktur und wird von user_data installiert. Die Pipeline ruft sie nur auf.
set -euo pipefail

version="${1#v}"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "Ungueltige Version: $version" >&2; exit 1; }

repo="$(cat /etc/tictactest/repo)"
dir=/opt/tictactest
jar="tictactest-$version.jar"

curl -fsSL -o "$dir/new-$jar" "https://github.com/$repo/releases/download/v$version/$jar"

# Probestart als Dienstbenutzer: ein Jar, das nicht einmal startet, darf die laufende Version nicht ersetzen.
# Ein intaktes Jar bricht wegen der leeren Eingabe mit einer Java-Exception ab, ein kaputtes mit einem Launcher-Fehler.
out="$(timeout 20 runuser -u tictactest -- /usr/bin/java -jar "$dir/new-$jar" </dev/null 2>&1 || true)"
if grep -Eq 'Unable to access jarfile|no main manifest|Could not find or load main class|Invalid or corrupt jarfile|UnsupportedClassVersionError' <<< "$out"; then
  echo "Das Jar $jar startet nicht, die laufende Version bleibt unveraendert:" >&2
  echo "$out" >&2
  rm -f "$dir/new-$jar"
  exit 1
fi

mv "$dir/new-$jar" "$dir/$jar"
ln -sfn "$dir/$jar" "$dir/current.jar"     # ueberlebt einen Neustart: derselbe Link, dieselbe Version
echo "$version" > /usr/share/nginx/html/version.txt
systemctl restart tictactest-console
echo "Version $version deployt"
