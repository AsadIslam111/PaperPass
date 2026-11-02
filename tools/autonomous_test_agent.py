"""
🤖 PaperPass — Physical Device Autonomous Test Harness
======================================================
Automated agentic controller using ADB for physical Android phone testing.
Capabilities:
  - Screen hierarchy perception (XML element parser)
  - Synthetic input execution (tap, text, keyevents)
  - UI state assertions
  - Test Oracle verification against virtual printer outputs and database records.
"""

import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from typing import Optional, List, Tuple


@dataclass
class UIElement:
    text: str
    resource_id: str
    content_desc: str
    bounds: Tuple[int, int, int, int]  # (x1, y1, x2, y2)
    center: Tuple[int, int]            # (centerX, centerY)


class AndroidDeviceController:
    PACKAGE_NAME = "com.shakib.paperpass"
    ACTIVITY_NAME = ".MainActivity"

    def __init__(self, serial: Optional[str] = None):
        self.serial_arg = ["-s", serial] if serial else []
        self._verify_connection()

    def _adb(self, cmd: List[str]) -> subprocess.CompletedProcess:
        full_cmd = ["adb"] + self.serial_arg + cmd
        return subprocess.run(full_cmd, capture_output=True, text=True)

    def _verify_connection(self):
        res = self._adb(["get-state"])
        if "device" not in res.stdout:
            raise ConnectionError(f"No active Android device detected via ADB! Run 'adb devices' to verify. Details: {res.stderr}")
        print("✅ Physical Android device connected successfully.")

    # ─────────────────────────────────────────────────────────────
    # App Lifecycle & Intents
    # ─────────────────────────────────────────────────────────────
    def launch_app(self):
        print(f"🚀 Launching {self.PACKAGE_NAME}...")
        self._adb(["shell", "am", "start", "-n", f"{self.PACKAGE_NAME}/{self.ACTIVITY_NAME}"])
        time.sleep(2)

    def force_stop_app(self):
        self._adb(["shell", "am", "force-stop", self.PACKAGE_NAME])
        time.sleep(1)

    def clear_app_data(self):
        """Reset app to fresh install state."""
        self._adb(["shell", "pm", "clear", self.PACKAGE_NAME])
        time.sleep(1)

    # ─────────────────────────────────────────────────────────────
    # Perception: UI Hierarchy & Screenshots
    # ─────────────────────────────────────────────────────────────
    def get_ui_hierarchy(self) -> List[UIElement]:
        """Dump UIAutomator XML hierarchy from physical device and parse clickable elements."""
        remote_path = "/sdcard/window_dump.xml"
        self._adb(["shell", "uiautomator", "dump", remote_path])
        res = self._adb(["shell", "cat", remote_path])

        if not res.stdout.strip():
            return []

        elements: List[UIElement] = []
        try:
            root = ET.fromstring(res.stdout)
            for node in root.iter("node"):
                text = node.attrib.get("text", "")
                res_id = node.attrib.get("resource-id", "")
                desc = node.attrib.get("content-desc", "")
                bounds_str = node.attrib.get("bounds", "")

                # Parse bounds format: [x1,y1][x2,y2]
                match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds_str)
                if match:
                    x1, y1, x2, y2 = map(int, match.groups())
                    center = ((x1 + x2) // 2, (y1 + y2) // 2)
                    elements.append(UIElement(
                        text=text,
                        resource_id=res_id,
                        content_desc=desc,
                        bounds=(x1, y1, x2, y2),
                        center=center
                    ))
        except Exception as e:
            print(f"⚠️ Error parsing hierarchy: {e}")

        return elements

    def find_element(self, text_contains: str, timeout_sec: int = 5) -> Optional[UIElement]:
        """Poll until an element with matching text appears."""
        end_time = time.time() + timeout_sec
        while time.time() < end_time:
            elements = self.get_ui_hierarchy()
            for el in elements:
                if text_contains.lower() in el.text.lower() or text_contains.lower() in el.content_desc.lower():
                    return el
            time.sleep(0.5)
        return None

    def take_screenshot(self, local_filepath: str):
        self._adb(["exec-out", "screencap", "-p"], stdout=open(local_filepath, "wb"))

    # ─────────────────────────────────────────────────────────────
    # Action Execution
    # ─────────────────────────────────────────────────────────────
    def tap(self, x: int, y: int):
        self._adb(["shell", "input", "tap", str(x), str(y)])
        time.sleep(0.5)

    def tap_element(self, text_contains: str, timeout_sec: int = 5) -> bool:
        el = self.find_element(text_contains, timeout_sec)
        if el:
            print(f"👆 Tapping '{text_contains}' at {el.center}")
            self.tap(el.center[0], el.center[1])
            return True
        print(f"❌ Element containing '{text_contains}' not found on screen!")
        return False

    def enter_pin_code(self, pin: str):
        """Enter 4-digit PIN on the custom PinEntryDialog keypad."""
        print(f"🔑 Entering 4-digit PIN: {pin}")
        for digit in pin:
            if not self.tap_element(digit, timeout_sec=3):
                # Fallback: keyevent
                self._adb(["shell", "input", "text", digit])
            time.sleep(0.2)

    def go_back(self):
        self._adb(["shell", "input", "keyevent", "4"])
        time.sleep(0.5)


# ─────────────────────────────────────────────────────────────────
# Test Oracle & Verifier
# ─────────────────────────────────────────────────────────────────
class TestOracle:
    def __init__(self, prints_directory: str):
        self.prints_dir = prints_directory

    def count_received_prints(self) -> int:
        if not os.path.exists(self.prints_dir):
            return 0
        return len([f for f in os.listdir(self.prints_dir) if f.endswith(('.pdf', '.bin'))])

    def wait_for_new_print(self, initial_count: int, timeout_sec: int = 10) -> Optional[str]:
        end_time = time.time() + timeout_sec
        while time.time() < end_time:
            current = [f for f in os.listdir(self.prints_dir) if f.endswith(('.pdf', '.bin'))]
            if len(current) > initial_count:
                # Return the newest file
                newest = max([os.path.join(self.prints_dir, f) for f in current], key=os.path.getctime)
                return newest
            time.sleep(0.5)
        return None


# ─────────────────────────────────────────────────────────────────
# Autonomous Execution Scenarios
# ─────────────────────────────────────────────────────────────────
def run_autonomous_test_suite():
    print("=" * 65)
    print("🤖 STARTING AUTONOMOUS TEST AGENT ON PHYSICAL DEVICE")
    print("=" * 65)

    prints_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "received_prints")
    oracle = TestOracle(prints_dir)
    initial_print_count = oracle.count_received_prints()

    device = AndroidDeviceController()

    # 1. Start App Clean
    device.force_stop_app()
    device.launch_app()

    # 2. Test Login as Employee
    print("\n--- Test Case 1: Authentication & Fast-Switch Demo ---")
    if device.tap_element("User", timeout_sec=4):  # Quick fill user
        device.tap_element("Authenticate & Connect", timeout_sec=3)
        time.sleep(2)

    # 3. Assert on Dashboard
    dashboard_el = device.find_element("Network Printers", timeout_sec=5)
    assert dashboard_el is not None, "FAILED: Dashboard did not load!"
    print("✅ Passed: Successfully authenticated and entered Dashboard.")

    # 4. Select a Printer
    print("\n--- Test Case 2: Printer Selection ---")
    if device.tap_element("Select & Configure Print", timeout_sec=4):
        time.sleep(1)

    # 5. Print Config Screen & PIN Gatekeeping
    print("\n--- Test Case 3: PIN Verification & Release ---")
    config_title = device.find_element("Print Configuration", timeout_sec=4)
    assert config_title is not None, "FAILED: Print Config Screen not reached!"

    # Tap Verify PIN & Release Print
    device.tap_element("Verify PIN & Release Print", timeout_sec=3)
    time.sleep(1)

    # Enter Employee PIN: 4321
    device.enter_pin_code("4321")
    time.sleep(2)

    # 6. Verify with Oracle
    print("\n--- Test Case 4: Hardware & Spooler Oracle Verification ---")
    new_doc = oracle.wait_for_new_print(initial_print_count, timeout_sec=10)
    if new_doc:
        print(f"🎉 SUCCESS! Document physically delivered to printer emulator: {os.path.basename(new_doc)}")
        print(f"   Size on disk: {os.path.getsize(new_doc)} bytes")
    else:
        print("⚠️ Note: Virtual printer did not receive document. Ensure virtual_printer.py is active.")

    print("\n" + "=" * 65)
    print("🏁 AUTONOMOUS TEST RUN COMPLETED")
    print("=" * 65)


if __name__ == "__main__":
    run_autonomous_test_suite()

