"""Lokaler Ersatz für nginx: liefert /version.txt mit CORS-Header (nur für die lokale Entwicklung).

Aufruf: python3 scripts/serve_version.py 1.2.0
"""
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

VERSION = sys.argv[1]


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(VERSION.encode())


HTTPServer(("127.0.0.1", 8081), Handler).serve_forever()
