"""
🖨️ PaperPass — PC Virtual Network Printer (IPP & RAW Socket Simulator)
======================================================================
This script simulates a real physical network printer on your PC/laptop.
It listens for incoming print jobs from PaperPass over:
  - Port 631: Internet Printing Protocol (IPP over HTTP)
  - Port 9100: RAW / JetDirect socket stream

Incoming documents are saved to the 'received_prints' folder.
Zero external dependencies required (uses 100% standard Python libraries).
"""

import http.server
import io
import os
import re
import socket
import struct
import sys
import threading
import time
from datetime import datetime

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "received_prints")
os.makedirs(OUTPUT_DIR, exist_ok=True)


def get_local_ip():
    """Detect LAN IPv4 address of this machine."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def sanitize_filename(name):
    clean = re.sub(r'[^a-zA-Z0-9_\-\.]', '_', name)
    return clean[:50] or "unnamed_document"


def parse_ipp_attributes(data):
    """
    Rudimentary parser for IPP binary operation attributes (RFC 8011).
    Extracts job-name, requesting-user-name, and document-format.
    """
    attributes = {}
    try:
        # IPP Header: version (2 bytes), op_id (2 bytes), req_id (4 bytes) = 8 bytes
        if len(data) < 9:
            return attributes

        idx = 8
        while idx < len(data):
            tag = data[idx]
            idx += 1
            if tag == 0x03:  # end-of-attributes-tag
                break

            if idx + 2 > len(data):
                break
            name_len = struct.unpack(">H", data[idx:idx+2])[0]
            idx += 2

            name = ""
            if name_len > 0 and idx + name_len <= len(data):
                name = data[idx:idx+name_len].decode('utf-8', errors='ignore')
                idx += name_len

            if idx + 2 > len(data):
                break
            val_len = struct.unpack(">H", data[idx:idx+2])[0]
            idx += 2

            val = ""
            if val_len > 0 and idx + val_len <= len(data):
                val = data[idx:idx+val_len].decode('utf-8', errors='ignore')
                idx += val_len

            if name:
                attributes[name] = val

        # Everything after end-of-attributes-tag (0x03) is document payload
        payload = data[idx:] if idx < len(data) else b""
        attributes["_payload"] = payload
    except Exception as e:
        attributes["_parse_error"] = str(e)
    return attributes


class IppRequestHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        client_ip = self.client_address[0]
        content_length = int(self.headers.get("Content-Length", 0))
        raw_body = self.rfile.read(content_length) if content_length > 0 else b""

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        print("\n" + "═" * 65)
        print(f"📥 [PRINT JOB RECEIVED] via IPP from {client_ip}")
        print(f"   Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"   Payload Size: {len(raw_body)} bytes ({len(raw_body) / 1024:.2f} KB)")

        parsed = parse_ipp_attributes(raw_body)
        job_name = parsed.get("job-name", "document")
        user_name = parsed.get("requesting-user-name", "anonymous")
        doc_format = parsed.get("document-format", "unknown")

        print(f"   User Name:    {user_name}")
        print(f"   Job Name:     {job_name}")
        print(f"   Format:       {doc_format}")

        # Save received document
        file_ext = ".pdf" if "pdf" in doc_format.lower() else ".bin"
        safe_name = sanitize_filename(f"{timestamp}_{user_name}_{job_name}") + file_ext
        save_path = os.path.join(OUTPUT_DIR, safe_name)

        payload_to_save = parsed.get("_payload", raw_body)
        if not payload_to_save:
            payload_to_save = raw_body

        with open(save_path, "wb") as f:
            f.write(payload_to_save)

        print(f"   💾 Saved to:  {save_path}")
        print("   ✅ Status:     200 OK (Job Spooled Successfully)")
        print("═" * 65 + "\n")

        # Construct standard IPP 2.0 successful-ok response (0x0000)
        # Version 2.0 (2 bytes) + Status 0x0000 (2 bytes) + Request-ID 1 (4 bytes) + End-tag (1 byte)
        ipp_response = struct.pack(">HHI", 0x0200, 0x0000, 1) + b"\x03"

        self.send_response(200)
        self.send_header("Content-Type", "application/ipp")
        self.send_header("Content-Length", str(len(ipp_response)))
        self.end_headers()
        self.wfile.write(ipp_response)

    def do_GET(self):
        # Health check endpoint for browsers or probe tests
        msg = f"PaperPass Virtual Printer is Online!\nTime: {datetime.now()}\nStorage: {OUTPUT_DIR}\n"
        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(msg)))
        self.end_headers()
        self.wfile.write(msg.encode())

    def log_message(self, format, *args):
        # Suppress noisy default HTTP log lines
        pass


def run_raw_socket_server(ip, port=9100):
    """Listens on port 9100 (JetDirect / RAW) in a background thread."""
    try:
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((ip, port))
        server.listen(5)
    except Exception as e:
        print(f"⚠️  RAW port {port} unavailable: {e}")
        return

    while True:
        try:
            client, addr = server.accept()
            print("\n" + "═" * 65)
            print(f"📥 [PRINT JOB RECEIVED] via RAW Socket Port {port} from {addr[0]}")
            data = b""
            while True:
                chunk = client.recv(4096)
                if not chunk:
                    break
                data += chunk
            client.close()

            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            save_path = os.path.join(OUTPUT_DIR, f"raw_{timestamp}.bin")
            with open(save_path, "wb") as f:
                f.write(data)
            print(f"   Payload Size: {len(data)} bytes")
            print(f"   💾 Saved to:  {save_path}")
            print("═" * 65 + "\n")
        except Exception:
            break


def main():
    lan_ip = get_local_ip()
    ipp_port = 631

    print("╔═══════════════════════════════════════════════════════════════╗")
    print("║        🖨️  PaperPass — PC Virtual Network Printer Server        ║")
    print("╚═══════════════════════════════════════════════════════════════╝")
    print(f"\n📡 Your PC's Local IP Address: \033[1;32m{lan_ip}\033[0m")
    print(f"📁 Received Prints Folder:    {OUTPUT_DIR}\n")

    # Start RAW Socket 9100 in background
    raw_thread = threading.Thread(target=run_raw_socket_server, args=("0.0.0.0", 9100), daemon=True)
    raw_thread.start()

    # Try binding to IPP Port 631
    try:
        httpd = http.server.HTTPServer(("0.0.0.0", ipp_port), IppRequestHandler)
    except PermissionError:
        ipp_port = 8631
        print(f"⚠️  Port 631 requires Admin privileges. Falling back to port {ipp_port}.")
        httpd = http.server.HTTPServer(("0.0.0.0", ipp_port), IppRequestHandler)

    print(f"🟢 IPP Print Server:        http://{lan_ip}:{ipp_port}/ipp/print")
    print(f"🟢 RAW JetDirect Server:    {lan_ip}:9100")
    print("\n" + "-" * 65)
    print("👉 HOW TO TEST FROM PAPERPPass APP:")
    print("   1. Open PaperPass on your phone / emulator.")
    print("   2. Sign in as Employee or Admin.")
    print("   3. On the Dashboard, tap '+ Add IP'.")
    print(f"   4. Name: 'My PC Virtual Printer', IP: '{lan_ip}', Dept: 'ALL'")
    print("   5. Select the printer, pick copies/pages, and enter your PIN!")
    print("-" * 65)
    print("\n⏳ Listening for incoming print jobs from PaperPass... (Press Ctrl+C to stop)\n")

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Virtual Printer Stopped.")


if __name__ == "__main__":
    main()

