import tkinter as tk
from tkinter import messagebox, simpledialog
import requests
import json
import os
from pathlib import Path
from typing import Optional, Dict, Any
try:
    import nfc
except ImportError:
    nfc = None  # nfcpy is optional; hardware integration requires it

# --- Configuration ---
# Configuration file allows persisting settings between runs
CONFIG_PATH = Path("nfc_config.json")

API_BASE_URL = "http://localhost:8080"  # Default, can be overridden by config
# For prototype: JWT can be hardcoded, or an input field can be added at startup
# This JWT should belong to an Entity Admin to manage sessions.
# The /nfc/scan endpoint itself just needs isAuthenticated(), so any valid JWT works there.
DEVICE_JWT_TOKEN: Optional[str] = None # Needs to be set for session management
CURRENT_SESSION_ID: Optional[int] = None
CURRENT_SESSION_PURPOSE: Optional[str] = None

load_config()

def load_config():
    global API_BASE_URL, DEVICE_JWT_TOKEN
    if CONFIG_PATH.exists():
        try:
            with open(CONFIG_PATH) as f:
                data = json.load(f)
                API_BASE_URL = data.get("api_base_url", API_BASE_URL)
                DEVICE_JWT_TOKEN = data.get("jwt_token", DEVICE_JWT_TOKEN)
        except Exception as e:
            print(f"Failed to load config: {e}")

def save_config():
    data = {
        "api_base_url": API_BASE_URL,
        "jwt_token": DEVICE_JWT_TOKEN,
    }
    try:
        with open(CONFIG_PATH, "w") as f:
            json.dump(data, f, indent=2)
    except Exception as e:
        print(f"Failed to save config: {e}")

# --- Global UI Elements (for easy update) ---
status_label: Optional[tk.Label] = None
session_purpose_label: Optional[tk.Label] = None
nfc_uid_entry: Optional[tk.Entry] = None
session_purpose_entry: Optional[tk.Entry] = None
start_session_button: Optional[tk.Button] = None
end_session_button: Optional[tk.Button] = None
jwt_entry_field: Optional[tk.Entry] = None # For manually entering JWT

# --- Helper Functions ---
def update_status(message: str, is_error: bool = False):
    if status_label:
        status_label.config(text=f"Status: {message}", fg="red" if is_error else "black")
    print(f"Status: {message}") # For non-GUI environments

def update_session_display():
    if session_purpose_label:
        if CURRENT_SESSION_ID and CURRENT_SESSION_PURPOSE:
            session_purpose_label.config(text=f"Active Session: {CURRENT_SESSION_PURPOSE} (ID: {CURRENT_SESSION_ID})")
            if end_session_button: end_session_button.config(state=tk.NORMAL)
            if start_session_button: start_session_button.config(state=tk.DISABLED)
        else:
            session_purpose_label.config(text="No active session")
            if end_session_button: end_session_button.config(state=tk.DISABLED)
            if start_session_button: start_session_button.config(state=tk.NORMAL)

def get_auth_headers() -> Dict[str, str]:
    if DEVICE_JWT_TOKEN:
        return {"Authorization": f"Bearer {DEVICE_JWT_TOKEN}", "Content-Type": "application/json"}
    return {"Content-Type": "application/json"}

def read_nfc_uid() -> Optional[str]:
    """Read an NFC tag UID using nfcpy if available."""
    if not nfc:
        update_status("nfcpy not installed", True)
        return None
    try:
        with nfc.ContactlessFrontend('usb') as clf:
            tag = clf.connect(rdwr={'on-connect': lambda tag: False})
            if tag:
                return tag.identifier.hex()
    except Exception as e:
        update_status(f"NFC read error: {e}", True)
    return None

# --- API Interaction Logic ---
def handle_start_session():
    global CURRENT_SESSION_ID, CURRENT_SESSION_PURPOSE, DEVICE_JWT_TOKEN

    if not DEVICE_JWT_TOKEN:
        messagebox.showerror("Error", "JWT Token not set. Please set it first.")
        update_status("JWT Token not set.", True)
        return

    purpose = session_purpose_entry.get() if session_purpose_entry else "N/A"
    if not purpose:
        messagebox.showerror("Error", "Session purpose cannot be empty.")
        return

    try:
        response = requests.post(
            f"{API_BASE_URL}/entity/sessions",
            headers=get_auth_headers(),
            json={"name": purpose} # Assuming 'name' is the DTO field for purpose
        )
        response.raise_for_status() # Raises HTTPError for bad responses (4XX or 5XX)

        session_data = response.json()
        CURRENT_SESSION_ID = session_data.get("id")
        CURRENT_SESSION_PURPOSE = session_data.get("name")

        if CURRENT_SESSION_ID:
            update_status(f"Session '{CURRENT_SESSION_PURPOSE}' started.")
            update_session_display()
        else:
            update_status("Failed to get session ID from response.", True)
            messagebox.showerror("Error", f"Failed to start session: Invalid response data\n{response.text}")

    except requests.exceptions.HTTPError as e:
        update_status(f"Error starting session: {e.response.status_code}", True)
        messagebox.showerror("Error", f"Failed to start session: {e.response.status_code}\n{e.response.text}")
    except requests.exceptions.RequestException as e:
        update_status(f"Connection error: {e}", True)
        messagebox.showerror("Error", f"Connection error: {e}")


def handle_end_session():
    global CURRENT_SESSION_ID, CURRENT_SESSION_PURPOSE, DEVICE_JWT_TOKEN
    if not CURRENT_SESSION_ID:
        messagebox.showinfo("Info", "No active session to end.")
        return
    if not DEVICE_JWT_TOKEN:
        messagebox.showerror("Error", "JWT Token not set.")
        update_status("JWT Token not set.", True)
        return

    try:
        response = requests.put(
            f"{API_BASE_URL}/entity/sessions/{CURRENT_SESSION_ID}/end",
            headers=get_auth_headers()
        )
        response.raise_for_status()

        update_status(f"Session '{CURRENT_SESSION_PURPOSE}' ended.")
        CURRENT_SESSION_ID = None
        CURRENT_SESSION_PURPOSE = None
        update_session_display()

    except requests.exceptions.HTTPError as e:
        update_status(f"Error ending session: {e.response.status_code}", True)
        messagebox.showerror("Error", f"Failed to end session: {e.response.status_code}\n{e.response.text}")
    except requests.exceptions.RequestException as e:
        update_status(f"Connection error: {e}", True)
        messagebox.showerror("Error", f"Connection error: {e}")


def handle_nfc_scan():
    if not CURRENT_SESSION_ID:
        messagebox.showwarning("Scan Error", "No active session. Please start a session first.")
        update_status("Scan failed: No active session.", True)
        return

    card_uid = nfc_uid_entry.get() if nfc_uid_entry else None
    if not card_uid:
        card_uid = read_nfc_uid()
        if not card_uid:
            messagebox.showwarning("Scan Error", "NFC Card UID cannot be read.")
            return

    try:
        payload = {"cardUid": card_uid} # Session ID is not sent in body for /nfc/scan as per backend
                                      # Backend determines session based on org of card holder

        # Note: /nfc/scan uses `isAuthenticated()`, so any valid token works.
        # If it needed EntityAdmin specifically for some reason, DEVICE_JWT_TOKEN would be critical.
        # Here, we use it if available, but it's less critical than for session start/end.
        response = requests.post(
            f"{API_BASE_URL}/nfc/scan",
            headers=get_auth_headers(),
            json=payload
        )

        if response.status_code == 201: # CREATED (typically first check-in)
            data = response.json() # Expecting "Checked in successfully to session: NAME at TIME" or similar text
            update_status(f"Scan success: {data}") # Or parse data if it's structured
            if nfc_uid_entry: nfc_uid_entry.delete(0, tk.END) # Clear entry
        elif response.status_code == 200: # OK (typically check-out or other success)
            data = response.json()
            update_status(f"Scan success: {data}")
            if nfc_uid_entry: nfc_uid_entry.delete(0, tk.END)
        else:
            # Handle other error codes specifically if needed
            error_message = response.text
            try: # Try to parse if backend sends JSON error
                error_data = response.json()
                if isinstance(error_data, dict) and "message" in error_data:
                    error_message = error_data["message"]
            except ValueError: # Not JSON
                pass
            update_status(f"Scan failed: {response.status_code} - {error_message}", True)
            messagebox.showerror("Scan Error", f"Scan failed: {response.status_code}\n{error_message}")

    except requests.exceptions.HTTPError as e: # Should be caught by status code check mostly
        update_status(f"Scan error: {e.response.status_code if e.response else 'Unknown'}", True)
        messagebox.showerror("Scan Error", f"Scan HTTP error: {e.response.status_code if e.response else 'Unknown'}\n{e.response.text if e.response else 'No response text'}")
    except requests.exceptions.RequestException as e:
        update_status(f"Connection error: {e}", True)
        messagebox.showerror("Scan Error", f"Connection error: {e}")

def set_jwt_token():
    global DEVICE_JWT_TOKEN
    token = jwt_entry_field.get() if jwt_entry_field else None
    if token:
        DEVICE_JWT_TOKEN = token
        update_status("JWT Token set successfully.")
        save_config()
    else:
        messagebox.showwarning("JWT Error", "JWT Token field is empty.")

def device_login():
    """Prompt for credentials and obtain a JWT from the backend."""
    username = simpledialog.askstring("Login", "Username:")
    password = simpledialog.askstring("Login", "Password:", show='*')
    if not username or not password:
        messagebox.showwarning("Login", "Username and password required")
        return
    try:
        response = requests.post(f"{API_BASE_URL}/admin/authenticate", json={"username": username, "password": password})
        response.raise_for_status()
        data = response.json()
        token = data.get("jwt")
        if token:
            global DEVICE_JWT_TOKEN
            DEVICE_JWT_TOKEN = token
            update_status("Device authenticated")
            save_config()
        else:
            update_status("Login failed: no token", True)
    except requests.RequestException as e:
        update_status(f"Login failed: {e}", True)

# --- GUI Setup ---
def setup_gui(root: tk.Tk):
    global status_label, session_purpose_label, nfc_uid_entry, session_purpose_entry
    global start_session_button, end_session_button, jwt_entry_field

    root.title("NFC Attendance System Client")
    root.geometry("500x450")

    # JWT Input Frame
    jwt_frame = tk.LabelFrame(root, text="JWT Configuration", padx=10, pady=10)
    jwt_frame.pack(padx=10, pady=10, fill="x")

    tk.Label(jwt_frame, text="Entity Admin JWT:").grid(row=0, column=0, sticky="w")
    jwt_entry_field = tk.Entry(jwt_frame, width=50)
    jwt_entry_field.grid(row=0, column=1, padx=5, pady=5)
    jwt_set_button = tk.Button(jwt_frame, text="Set JWT", command=set_jwt_token)
    jwt_set_button.grid(row=0, column=2, padx=5)
    login_button = tk.Button(jwt_frame, text="Login", command=device_login)
    login_button.grid(row=0, column=3, padx=5)


    # Session Management Frame
    session_frame = tk.LabelFrame(root, text="Session Management", padx=10, pady=10)
    session_frame.pack(padx=10, pady=10, fill="x")

    tk.Label(session_frame, text="Session Purpose:").grid(row=0, column=0, sticky="w", pady=5)
    session_purpose_entry = tk.Entry(session_frame, width=30)
    session_purpose_entry.grid(row=0, column=1, sticky="ew", padx=5, pady=5)

    start_session_button = tk.Button(session_frame, text="Start Session", command=handle_start_session)
    start_session_button.grid(row=0, column=2, padx=5, pady=5)

    end_session_button = tk.Button(session_frame, text="End Session", command=handle_end_session, state=tk.DISABLED)
    end_session_button.grid(row=1, column=2, padx=5, pady=5, sticky="e")

    session_purpose_label = tk.Label(session_frame, text="No active session", font=("Arial", 10, "italic"))
    session_purpose_label.grid(row=1, column=0, columnspan=2, sticky="w", pady=5)


    # NFC Scan Frame
    scan_frame = tk.LabelFrame(root, text="NFC Card Scan (Mock)", padx=10, pady=10)
    scan_frame.pack(padx=10, pady=10, fill="x")

    tk.Label(scan_frame, text="Card UID:").grid(row=0, column=0, sticky="w", pady=5)
    nfc_uid_entry = tk.Entry(scan_frame, width=30)
    nfc_uid_entry.grid(row=0, column=1, sticky="ew", padx=5, pady=5)

    scan_button = tk.Button(scan_frame, text="Scan Card", command=handle_nfc_scan)
    scan_button.grid(row=0, column=2, padx=5, pady=5)

    # Status Display
    status_label = tk.Label(root, text="Status: Idle", relief=tk.SUNKEN, anchor="w", padx=5)
    status_label.pack(side=tk.BOTTOM, fill="x", pady=5, padx=10)

    update_session_display() # Initial state for buttons

# --- Main Application ---
if __name__ == "__main__":
    # This part will not run effectively in the non-GUI environment
    # but is structured for a local Tkinter application.
    try:
        root = tk.Tk()
        setup_gui(root)
        # Example pre-filled JWT for easier testing if needed (REMOVE FOR PRODUCTION)
        # if jwt_entry_field:
        #    jwt_entry_field.insert(0, "YOUR_TEST_JWT_HERE")
        root.mainloop()
    except ImportError:
        print("Tkinter is not available or could not be initialized in this environment.")
        print("This application requires a GUI environment with Tkinter installed.")
        print("The core logic (API calls, etc.) is defined above and can be tested separately if needed.")
    except tk.TclError as e:
        print(f"Tkinter TclError: {e}. This usually means no display server is available.")
        print("Consider running in a desktop environment or using X11 forwarding if on a remote server.")

    # Fallback for non-GUI environments (e.g., for testing logic)
    if "root" not in locals() or not isinstance(root, tk.Tk):
        print("\n--- Non-GUI Mode ---")
        print("Simulating some actions (if JWT is provided manually):")
        # Example: Manually set a JWT for testing logic if no GUI
        # DEVICE_JWT_TOKEN = "some_hardcoded_jwt_for_testing_logic"
        # if DEVICE_JWT_TOKEN:
        #     print(f"Test JWT set to: {DEVICE_JWT_TOKEN[:20]}...")
        #     # Simulate starting a session (requires session_purpose_entry to be mocked or value provided)
        #     # handle_start_session() # This would fail as UI elements are None
        #     # Simulate scanning a card (requires nfc_uid_entry to be mocked or value provided)
        #     # handle_nfc_scan() # This would fail
        # else:
        #     print("No JWT provided for non-GUI mode logic testing.")
        print("GUI elements are not initialized. Cannot run full application flow.")

```
