# 🖨️ PaperPass — Secure Network Printer Discovery & Access Management

> **Tagline:** *Zero Unauthorized Prints. Total LAN Visibility.*  
> **Target Audience:** Corporate Offices, Educational Labs, Co-working Spaces, Enterprise IT  
> **Primary Roles Targeted:** Network Engineer, NOC Analyst, IT Officer / IT Executive, Android Solutions Developer

---

## 📌 1. Project Overview

**PaperPass** is a production-grade Android application designed to discover network-attached printers on a local area network (LAN) and enforce **strict authentication, role-based access control (RBAC), and usage quotas** before any document is physically released to print.

In traditional office setups, any device connected to the office WiFi can broadcast print jobs, causing:
1. **Paper & Toner Waste:** Unmonitored personal and accidental prints.
2. **Confidentiality Breaches:** Sensitive documents sitting unattended on printer trays.
3. **Bandwidth & Device Abuse:** Unregistered guests and unauthorized staff over-utilizing heavy-duty office printers.

**PaperPass solves this** by acting as an intelligent mobile gateway and policy enforcer:
- **Discovers** LAN printers dynamically via **mDNS / Bonjour (Android NSD)**.
- **Authenticates** users via Employee ID + Password.
- **Secures** print jobs using an on-demand **4-digit PIN confirmation** or Admin Approval.
- **Enforces** department restrictions and daily/monthly page quotas.
- **Audits** all print operations for IT reporting.

---

## 🏗️ 2. High-Level System Architecture

```mermaid
flowchart TD
    subgraph Client ["📱 PaperPass Android Client (MVVM + Clean Architecture)"]
        UI["UI Layer (Jetpack Compose + Material 3)"]
        VM["ViewModel & StateFlow"]
        UC["UseCases / Domain Layer"]
        REPO["Repositories"]
        
        UI --> VM
        VM --> UC
        UC --> REPO
    end

    subgraph DataSources ["💾 Local & Remote Data Layers"]
        ROOM["Room Database (Encrypted SQLite)\nUsers, Quotas, Logs, Cached Printers"]
        NSD["Android NSD Manager (mDNS / DNS-SD)\nService: _ipp._tcp / _printer._tcp"]
        IPP["IPP Client (Internet Printing Protocol)\nBinary Post to port 631 / 9100 RAW"]
        SNMP["SNMP / HTTP Query\nToner Levels, Paper Tray, Error Flags"]
        
        REPO --> ROOM
        REPO --> NSD
        REPO --> IPP
        REPO --> SNMP
    end

    subgraph NetworkEnv ["🏢 Corporate LAN / Subnet"]
        PRN1["🟢 HP LaserJet Pro (192.168.10.25)"]
        PRN2["🟢 Canon imageRUNNER (192.168.10.26)"]
        PRN3["🔴 Epson WorkForce (192.168.20.15)"]
        
        NSD -.->|Discover Multicast| PRN1
        NSD -.->|Discover Multicast| PRN2
        IPP -->|Send IPP Job| PRN1
        IPP -->|Send IPP Job| PRN2
        SNMP -->|Status Poll| PRN1
    end
```

---

## 🔐 3. Access Control & Authorization (RBAC)

```mermaid
flowchart TD
    Start["User Opens PaperPass"] --> Login["Login (Employee ID + Password)"]
    Login --> RoleCheck{"Identify Role"}

    RoleCheck -->|Admin| AdminFlow["Admin Rights\n- Full printer visibility\n- Set quotas & create users\n- Approve guest requests\n- View audit logs"]
    RoleCheck -->|Employee / User| UserFlow["Employee Rights\n- Filter printers by department\n- Select document (PDF/Img)\n- Check remaining quota"]
    RoleCheck -->|Guest| GuestFlow["Guest Rights\n- Discover lobby/guest printers\n- Submit print request\n- Await Admin approval"]

    UserFlow --> QuotaCheck{"Quota Available?"}
    QuotaCheck -->|No| QuotaBlock["❌ Blocked: Quota Exceeded\nPrompt to request IT admin boost"]
    QuotaCheck -->|Yes| PinPrompt["🔑 Enter 4-Digit Security PIN"]
    
    PinPrompt --> ValidatePIN{"PIN Correct?"}
    ValidatePIN -->|No| PinError["❌ Invalid PIN"]
    ValidatePIN -->|Yes| DispatchJob["🖨️ Transmit Job via IPP"]

    GuestFlow --> SendReq["Send Print Request to Admin Queue"]
    SendReq --> AdminAction{"Admin Action"}
    AdminAction -->|Approved| DispatchJob
    AdminAction -->|Rejected| RejectNotice["❌ Request Denied by Admin"]

    DispatchJob --> UpdateQuota["Deduct Quota & Write to Audit Log"]
```

### Role Matrix

| Capability | Admin | Employee (User) | Guest |
| :--- | :---: | :---: | :---: |
| **Scan LAN for Printers** | ✅ All Subnets | ✅ Assigned Dept | ✅ Guest Subnet |
| **Print Directly (with PIN)** | ✅ Unlimited | ✅ Within Quota | ❌ (Request Only) |
| **Bypass Quota Limits** | ✅ Yes | ❌ No | ❌ No |
| **Approve Guest Requests** | ✅ Yes | ❌ No | ❌ No |
| **Manage Users & Reset PINs** | ✅ Yes | ❌ No | ❌ No |
| **Export Audit Logs (CSV)** | ✅ Yes | ❌ No | ❌ No |

---

## 📱 4. Key UI Screens & Wireframes

### Screen 1: Login & Identity Verification
* **Purpose:** Ensure only authorized personnel connect to printer queues.
* **UI Elements:**
  * App Banner: `PaperPass` with stylized lock + printer emblem.
  * Inputs: `Employee ID / Username`, `Password`.
  * Biometric Prompt option (Fingerprint / Face Unlock).
  * Quick links: *Guest Access Request*, *Forgot PIN*.

### Screen 2: Central Dashboard
* **Metrics Header:**
  * Network Status: Connected SSID, Gateway IP, Active Subnet.
  * Personal Quota Progress Bar: `38 / 50 pages used today`.
  * Quick Status Chips: `4 Printers Online`, `1 Needs Paper`.
* **Discovered Printers Carousel / List:**
  * Card view showing: Printer Name, IP, Department tag (`HR`, `Engineering`, `General`), Ink gauge, Online/Offline badge.
* **Floating Action Button (FAB):** `+ Quick Print Document`.

### Screen 3: Print Configuration & Security Release
* **File Preview:** File name, file size, thumbnail, calculated page count.
* **Print Settings:**
  * Copies selector (`-` `+`).
  * Color Profile: Monochrome (B&W) / Full Color.
  * Duplex: Single-sided / Double-sided (Long Edge / Short Edge).
  * Page Range: `All` or comma-separated `1-3, 5`.
* **Security Check:**
  * Dynamic calculation: `Remaining: 12 pages` $\rightarrow$ `After Job: 8 pages`.
  * 4-Digit Boxed PIN Entry widget.
  * Button: `[ 🖨️ Authorize & Print ]`.

### Screen 4: IT Administrator Console
* **Navigation Tabs:** `Overview`, `Users`, `Printer ACLs`, `Pending Requests`.
* **Pending Requests Queue:** Shows live guest requests with document name, requester name, timestamp, and instant `[Approve]` / `[Decline]` triggers.
* **Audit Trail Table:** Real-time stream of `{timestamp, user, printer, page_count, status}`.

---

## 🗄️ 5. Database Schema (Room / SQLite)

```mermaid
erDiagram
    USERS ||--o{ PRINT_JOBS : places
    USERS ||--|| QUOTAS : allocated
    PRINTERS ||--o{ PRINT_JOBS : receives

    USERS {
        int id PK
        string employee_id UK
        string full_name
        string email
        string password_hash
        string security_pin
        string role "ADMIN | USER | GUEST"
        string department
        boolean is_active
        datetime created_at
    }

    PRINTERS {
        int id PK
        string name
        string ip_address
        int port
        string mac_address
        string model
        string department_restriction
        string status "ONLINE | OFFLINE | BUSY | ERROR"
        int black_toner_pct
        int color_toner_pct
        boolean paper_tray_empty
        datetime last_seen
    }

    QUOTAS {
        int id PK
        int user_id FK
        int daily_limit
        int monthly_limit
        int pages_used_today
        int pages_used_month
        date last_reset_date
    }

    PRINT_JOBS {
        int id PK
        int user_id FK
        int printer_id FK
        string document_name
        int page_count
        int copies
        string color_mode "COLOR | MONO"
        boolean is_duplex
        string status "PENDING | APPROVED | PRINTING | COMPLETED | REJECTED | FAILED"
        string error_message
        datetime submitted_at
        datetime completed_at
    }
```

---

## 📁 6. Android Project Directory Structure

```
PaperPass/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/shakib/paperpass/
│       │   │   ├── PaperPassApp.kt                # Application class (Hilt initialization)
│       │   │   ├── MainActivity.kt                # Single activity hosting Compose Nav
│       │   │   │
│       │   │   ├── core/                          # Cross-cutting utilities
│       │   │   │   ├── network/                   # Network monitoring & connectivity state
│       │   │   │   │   ├── NetworkMonitor.kt
│       │   │   │   │   └── SubnetScanner.kt
│       │   │   │   ├── security/                  # Cryptography & Hashing
│       │   │   │   │   ├── PasswordHasher.kt      # BCrypt / PBKDF2
│       │   │   │   │   └── PinValidator.kt
│       │   │   │   └── util/
│       │   │   │       ├── Resource.kt
│       │   │   │       └── Extensions.kt
│       │   │   │
│       │   │   ├── data/                          # Data Layer implementation
│       │   │   │   ├── local/
│       │   │   │   │   ├── AppDatabase.kt
│       │   │   │   │   ├── dao/
│       │   │   │   │   │   ├── UserDao.kt
│       │   │   │   │   │   ├── PrinterDao.kt
│       │   │   │   │   │   ├── PrintJobDao.kt
│       │   │   │   │   │   └── QuotaDao.kt
│       │   │   │   │   └── entity/
│       │   │   │   │       ├── UserEntity.kt
│       │   │   │   │       ├── PrinterEntity.kt
│       │   │   │   │       ├── PrintJobEntity.kt
│       │   │   │   │       └── QuotaEntity.kt
│       │   │   │   ├── network/
│       │   │   │   │   ├── nsd/                   # mDNS / DNS-SD Service Discovery
│       │   │   │   │   │   ├── NsdPrinterDiscovery.kt
│       │   │   │   │   │   └── PrinterServiceInfoParser.kt
│       │   │   │   │   ├── ipp/                   # IPP binary protocol handler
│       │   │   │   │   │   ├── IppClient.kt
│       │   │   │   │   │   └── IppJobBuilder.kt
│       │   │   │   │   └── snmp/                  # Printer hardware telemetry
│       │   │   │   │       └── SnmpManager.kt
│       │   │   │   └── repository/
│       │   │   │       ├── AuthRepositoryImpl.kt
│       │   │   │       ├── PrinterRepositoryImpl.kt
│       │   │   │       ├── PrintJobRepositoryImpl.kt
│       │   │   │       └── QuotaRepositoryImpl.kt
│       │   │   │
│       │   │   ├── domain/                        # Business Logic & Interfaces
│       │   │   │   ├── model/
│       │   │   │   │   ├── User.kt
│       │   │   │   │   ├── Printer.kt
│       │   │   │   │   ├── PrintJob.kt
│       │   │   │   │   └── Quota.kt
│       │   │   │   ├── repository/
│       │   │   │   │   ├── AuthRepository.kt
│       │   │   │   │   ├── PrinterRepository.kt
│       │   │   │   │   ├── PrintJobRepository.kt
│       │   │   │   │   └── QuotaRepository.kt
│       │   │   │   └── usecase/
│       │   │   │       ├── DiscoverPrintersUseCase.kt
│       │   │   │       ├── AuthenticateUserUseCase.kt
│       │   │   │       ├── AuthorizeAndPrintUseCase.kt
│       │   │   │       ├── EvaluateQuotaUseCase.kt
│       │   │   │       └── ManageGuestRequestsUseCase.kt
│       │   │   │
│       │   │   ├── presentation/                  # UI Components (Jetpack Compose)
│       │   │   │   ├── navigation/
│       │   │   │   │   ├── Screen.kt
│       │   │   │   │   └── NavGraph.kt
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Color.kt
│       │   │   │   │   ├── Theme.kt
│       │   │   │   │   └── Type.kt
│       │   │   │   ├── components/
│       │   │   │   │   ├── PrinterCard.kt
│       │   │   │   │   ├── PinKeypadDialog.kt
│       │   │   │   │   ├── TonerGauge.kt
│       │   │   │   │   ├── QuotaBadge.kt
│       │   │   │   │   └── CustomTopAppBar.kt
│       │   │   │   └── screens/
│       │   │   │       ├── auth/
│       │   │   │       │   ├── LoginScreen.kt
│       │   │   │       │   └── LoginViewModel.kt
│       │   │   │       ├── dashboard/
│       │   │   │       │   ├── DashboardScreen.kt
│       │   │   │       │   └── DashboardViewModel.kt
│       │   │   │       ├── print/
│       │   │   │       │   ├── PrintConfigScreen.kt
│       │   │   │       │   └── PrintConfigViewModel.kt
│       │   │   │       ├── history/
│       │   │   │       │   ├── PrintHistoryScreen.kt
│       │   │   │       │   └── PrintHistoryViewModel.kt
│       │   │   │       └── admin/
│       │   │   │           ├── AdminPanelScreen.kt
│       │   │   │           ├── UserManagementTab.kt
│       │   │   │           ├── RequestsApprovalTab.kt
│       │   │   │           └── AdminViewModel.kt
│       │   │   │
│       │   │   └── di/                            # Dependency Injection (Hilt)
│       │   │       ├── DatabaseModule.kt
│       │   │       ├── NetworkModule.kt
│       │   │       └── RepositoryModule.kt
│       │   │
│       │   └── res/
│       │       ├── drawable/
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   └── colors.xml
│       │       └── xml/
│       │           └── network_security_config.xml
│       │
│       └── test/                                  # Unit tests (Robolectric / MockK)
├── build.gradle.kts
├── settings.gradle.kts
├── plan.md                                        # This architecture master plan
└── README.md
```

---

## 🛠️ 7. Detailed Tech Stack & Dependencies

| Area | Component | Implementation / Library | Justification |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin 1.9+ | Modern, concise, null-safe | Android standard |
| **UI Framework** | Jetpack Compose | Material 3 Components | Modern declarative UI, effortless reactive updates |
| **Architecture** | Clean Architecture | MVVM + Repository Pattern | Decoupled UI and business logic for easy testing |
| **Dependency Injection**| Dagger Hilt | `hilt-android`, `hilt-compiler` | Industry standard, modular testability |
| **Local Persistence** | Room Database | `androidx.room:room-ktx` | Type-safe SQLite persistence with Flow integration |
| **Device Discovery** | Android NSD API | `android.net.nsd.NsdManager` | Native zero-configuration mDNS discovery |
| **Print Protocol** | IPP Client | Binary IPP parser / `jipp` | RFC 8011 compliant printing over IP |
| **Security & Auth** | Hashing & Biometrics | BCrypt / `androidx.biometric` | Secure credential hashing and hardware fingerprint auth |
| **Asynchronous Engine**| Coroutines & Flow | `kotlinx-coroutines-core` | Non-blocking socket I/O and reactive UI streams |
| **Document Parsing** | Android Storage Access | `ActivityResultContracts.GetContent` | Safe handling of PDF / Image streams |

---

## 📅 8. Step-by-Step Implementation Roadmap

```mermaid
gantt
    title PaperPass Implementation Milestones
    dateFormat  YYYY-MM-DD
    section Phase 1: Setup & Data
    Project scaffolding & Gradle setup    :2026-11-03, 4d
    Room DB entities & DAOs               :2026-11-07, 4d
    Authentication & Security layer       :2026-11-11, 4d
    section Phase 2: Network & Discovery
    Android NSD mDNS Discovery service    :2026-11-15, 5d
    Manual IP / Subnet fallback scanner   :2026-11-20, 3d
    Printer telemetry (SNMP / status)     :2026-11-23, 4d
    section Phase 3: Print Pipeline
    File Picker & PDF page count parser   :2026-11-27, 4d
    IPP protocol socket connection        :2026-12-01, 6d
    Print queue & retry manager           :2026-12-07, 4d
    section Phase 4: Policy & UI
    RBAC & Quota evaluation engine        :2026-12-11, 5d
    4-Digit PIN confirmation dialog       :2026-12-16, 3d
    Admin Dashboard & Guest Approvals     :2026-12-19, 6d
    section Phase 5: Polish & Showcase
    UI polish, dark mode & animations     :2026-12-25, 4d
    Documentation, demo video & GitHub    :2026-12-29, 5d
```

---

## 💼 9. Resume & Interview Value Proposition

### How to Describe PaperPass on Your Resume:
> **PaperPass — Enterprise Mobile Print & Access Management Platform (Android / Kotlin)**  
> *Developed a secure Android network printing application featuring mDNS-based automated printer discovery and role-based access control (RBAC).*  
> - Designed an mDNS service discovery engine utilizing Android's `NsdManager` to detect LAN printers across subnets without hardcoded IPs.  
> - Implemented an access gatekeeper enforcing 4-digit PIN confirmation, department-level ACLs, and quota limits to eliminate unauthorized and accidental prints.  
> - Engineered an IPP (Internet Printing Protocol) driver pipeline to serialize PDF and document payloads directly to network-attached printers over TCP port 631.  
> - Built an Administrative dashboard managing real-time guest print approvals, device telemetry (toner/paper status), and compliance audit trails.  
> - **Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room DB, Hilt, Coroutines/Flow, Android NSD, IPP RFC 8011, BCrypt.

### Questions Interviewers Will Ask You:
1. **"How does mDNS discovery work in an enterprise subnet that blocks multicast?"**  
   *Answer:* "Standard mDNS uses multicast address `224.0.0.251` on UDP port `5353`, which routers usually don't cross between VLANs. In PaperPass, I implemented Android's `NsdManager` for local broadcast domains, but added an explicit **Manual IP & Direct Subnet Scan fallback** using socket connectivity tests to bridge cross-VLAN printers."
2. **"How do you prevent someone from tampering with their quota on their phone?"**  
   *Answer:* "In the local MVP version, the database can use SQLCipher with Android Keystore backed encryption. In a multi-user corporate deployment, the app syncs against a central verification endpoint, while the local Room DB acts as an offline cache with cryptographic checksums."

