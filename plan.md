Listed directory logapp
Viewed srs.md:1-800
Viewed srs.md:801-1600
Listed directory logapp
Viewed build.gradle.kts:1-74
Viewed srs.md:1601-2400
Listed directory data
Listed directory di
Listed directory ui
Listed directory utils
Listed directory service
Viewed srs.md:2401-3200
Listed directory model
Listed directory database
Listed directory repository
Listed directory auth
Viewed srs.md:3201-3525
Viewed SessionRepositoryImpl.kt:1-25
Listed directory dao
Listed directory usecase
Listed directory tracking
Listed directory analytics
Listed directory theme

Dựa trên việc phân tích chi tiết tài liệu `srs.md` và mã nguồn hiện tại của dự án trong thư mục `d:\logapp`, dưới đây là **Bản đồ lộ trình triển khai chi tiết (Implementation Roadmap)** nhằm đưa hệ thống từ trạng thái hiện tại đạt mức độ hoàn thiện 100% theo SRS.

---

# PART A — Executive Summary

* **Current Project State:** Dự án hiện đang ở giai đoạn khởi tạo (foundation phase). Đã thiết lập cấu trúc cơ bản với Jetpack Compose, Hilt, Room và DataStore. Đã có một số Entity (Activity, Session, Note, SyncMetadata) và một số Use Case cơ bản (Start/Stop/Add/Delete Activity). Màn hình duy nhất hiện có là `HomeScreen`.
* **SRS Size/Scope:** Lớn. Hệ thống không chỉ đơn thuần là tracking, mà còn bao gồm các Business Engine phức tạp (Overlap, Analytics, Timeline), cơ chế đồng bộ Firebase (Cloud Sync), cơ chế Backup Google Drive (File Backup), hệ thống Import/Export/Restore và các yêu cầu khắt khe về Non-Functional (Offline-first, Background Sync, Zero-friction Tracking).
* **Major Implementation Gaps:** 
  * Thiếu hoàn toàn tầng Domain Logic cho Analytics, Overlap, và Concurrency.
  * Thiếu hoàn toàn UI cho Timeline, Analytics, Export/Import, Settings, Backup.
  * Thiếu toàn bộ tích hợp Firebase (Auth, Firestore, Crashlytics) và Google Drive.
  * Thiếu hệ thống Serialization (JSON) và WorkManager cho Background Tasks.
* **Major Architectural Gaps:** Hiện tại project đang gộp chung Use Cases vào package `utils/usecase`, thiếu sự tách biệt rõ ràng của một lớp `domain` độc lập như SRS yêu cầu.
* **Major Risks:** Thuật toán tính toán Overlap với số lượng lớn session, quản lý Conflict khi đồng bộ (Sync) và việc quản lý 2 luồng backup (Firebase + Drive) độc lập đòi hỏi thiết kế transaction rất chặt chẽ.

---

# PART B — Current Architecture

Sau khi inspect source code (chứng cứ: thư mục `data`, `di`, `ui`, `utils/usecase`), kiến trúc thực tế hiện tại là:

```text
UI (HomeScreen, HomeViewModel)
       ↓
Use Case (StartActivityUseCase, StopActivityUseCase...)
       ↓
Repository (ActivityRepositoryImpl, SessionRepositoryImpl)
       ↓
DAO (ActivityDao, SessionDao)
       ↓
Room Database (TimeLensDatabase)
```
**Nhận xét:** Đã làm đúng hướng (áp dụng UDF, Repository Pattern). Nhưng thiếu sự tách biệt của Core/Domain (Use case đang bị đẩy vào `utils`). Các tính năng Cloud và Backup chưa hề tồn tại.

---

# PART C — Target Architecture

Dựa trên SRS, hệ thống cần tiến tới cấu trúc:

```text
UI (Compose)
       ↓
ViewModels / StateFlow
       ↓
Domain Use Cases & Engines (Time, Overlap, Analytics)
       ↓
Repositories (Activity, Session, Backup, CloudSync)
       ↓
 ┌─────────────┴─────────────┐
 │                           │
Room (SOURCE OF TRUTH)       │
                             │
 ┌───────────────────────────┴───────────────────────────┐
 │                                                       │
 Firebase Sync (WorkManager)                            Export/Backup Engine
 │                                                       │
 Firestore                                          JSON / Google Drive
```

| Area | Current | Target | Change required |
| ---- | ------- | ------ | --------------- |
| Directory Structure | Theo layer (`data`, `ui`, `utils`) | Theo feature hoặc Clean Arch chuẩn (`core`, `domain`, `data`, `feature`) | Cần refactor cấu trúc package ngay lập tức. |
| Business Logic | Nằm rải rác trong `utils/usecase` | Các Engine độc lập (OverlapEngine, AnalyticsEngine) | Tạo các Domain Engine và viết Unit Tests. |
| Background Jobs | Không có | WorkManager | Cần tích hợp WorkManager. |
| Dependencies | Chỉ có Compose, Room, Hilt, DataStore | Cần thêm Firebase, Google Drive API, kotlinx.serialization, WorkManager | Bổ sung vào `build.gradle.kts`. |

---

# PART D — Complete Requirement Inventory

* **REQ-TRACK-001**: Bắt đầu một Activity.
* **REQ-TRACK-002**: Dừng một Activity đang chạy.
* **REQ-TRACK-003**: Cho phép nhiều Activity chạy đồng thời (Overlap).
* **REQ-TRACK-004**: Offline tracking (Room là Source of Truth).
* **REQ-TRACK-005**: Khôi phục Active Session khi App restart.
* **REQ-TRACK-006**: Timer dựa trên timestamp, không phải process background.
* **REQ-WIDGET-001**: Android App Widget & Notifications (Compact/Active).
* **REQ-EDIT-001**: Sửa, xóa, tạo thủ công session. Split và Merge session.
* **REQ-TIME-001**: Timeline hiển thị theo ngày, zoom, scroll.
* **REQ-ENG-001**: Engine tính Overlap Duration.
* **REQ-ENG-002**: Engine tính Concurrency (1, 2, 3, 4+ activities).
* **REQ-ANALYTICS-001**: Daily, Weekly, Monthly Analytics.
* **REQ-ANALYTICS-002**: Activity Statistics (longest, shortest, average...).
* **REQ-SYNC-001**: Firebase Authentication (Google Sign-In).
* **REQ-SYNC-002**: Firebase Firestore Sync (Local-first, queue via WorkManager).
* **REQ-SYNC-003**: Quản lý SyncMetadata, Soft Delete và Conflict Resolution.
* **REQ-DRIVE-001**: Xác thực Google Drive (quyền `drive.file`).
* **REQ-DRIVE-002**: Backup dữ liệu sang JSON và upload Drive.
* **REQ-DRIVE-003**: Restore từ JSON, preview, merge/replace.
* **REQ-EXPORT-001**: Export ra CSV, JSON (có Schema version).
* **REQ-SET-001**: Cấu hình (Theme, Day boundary, Sync logic).
* **REQ-NFR-001**: Privacy, Crashlytics, Data Integrity checking.

---

# PART E — Requirement Traceability Matrix

| Requirement | SRS Location | Current implementation | Status | Missing work |
| --- | --- | --- | --- | --- |
| Start/Stop Activity | #9, #85 | `StartActivityUseCase`, `StopActivityUseCase` | PARTIAL | Thiếu xử lý SyncMetadata khi Start/Stop. Thiếu validate. |
| Multiple Sessions | #9.3, #77 | Room cho phép nhưng UI có thể chưa tối ưu. | PARTIAL | Engine xử lý Overlap chưa có, Timeline chưa vẽ được. |
| Timer calculation | #10 | `HomeScreen.kt` | PARTIAL | Cần review logic đếm thời gian trong UI, chưa có Widget. |
| Manual Session Edit | #14 | Chưa có UI và UseCase. | MISSING | Domain, UseCase, UI. |
| Timeline View | #15 | Thư mục `timeline` trống. | MISSING | Domain model, Canvas/Compose UI, Zoom/Scroll logic. |
| Analytics Engines | #18, #20 | Thư mục `analytics` trống. | MISSING | OverlapEngine, ConcurrencyEngine, AnalyticsModel, Unit Tests. |
| JSON Export/Import | #28, #29, #53 | Không có `kotlinx.serialization`. | MISSING | Models, Serialization logic, Validation Engine, UI. |
| Google Drive | #33 - #39 | Không có dependencies, thư mục `backup` trống. | MISSING | Auth, Drive API integration, Backup Rotation, Restore. |
| Firebase Sync | #42 - #50 | Không có firebase BOM. `SyncMetadataEntity` có tồn tại. | PARTIAL | Auth, Firestore logic, WorkManager queue, Conflict Engine. |
| Data Health / Settings | #57, #74 | Không có màn hình Settings. | MISSING | Integrity Check logic, Settings UI, DataStore integration. |

---

# PART F — Dependency Graph

```text
Build Config (Add Firebase, serialization, WorkManager, Drive)
       ↓
Architecture Refactor (Move utils/usecase -> domain/usecase)
       ↓
Domain Foundation (Time Engine, Overlap Engine, Validation Engine)
       ↓
Database Enhancements (SyncMetadata logic, Soft Delete)
       ↓
Core Tracking Features (Manual Edit, Split, Merge)
       ↓
Analytics Engine & Business Logic (Daily, Weekly, Monthly)
       ↓
UI Implementation (Timeline, Analytics, Settings)
       ↓
Serialization Layer (JSON Schema, CSV Generator)
       ↓
Export / Import (Local files)
       ↓
Cloud Sync (Firebase Auth, Firestore, WorkManager)
       ↓
Google Drive (Auth, Backup Manager, Restore Engine)
       ↓
Widget & Notifications
       ↓
Hardening (Crashlytics, E2E Tests, Data Integrity)
```

---

# PART G — Phase Roadmap

### Phase 1 — Project Structure & Build Baseline
* **Objective:** Chuẩn hóa kiến trúc và tích hợp các thư viện nền tảng để sẵn sàng cho các engine lớn.
* **Prerequisites:** Trạng thái hiện tại.
* **Requirements covered:** N/A (Foundation).
* **Current state:** Các thư viện nâng cao (Serialization, WorkManager, Firebase, Google API) chưa có. Cấu trúc thư mục chưa chuẩn Clean.
* **Gap:** Thiếu config, sai cấu trúc.
* **Tasks:**
  * TASK-01.1: Refactor thư mục (tạo `core`, `domain`, `feature`). Di chuyển `utils/usecase` sang `domain/usecase`.
  * TASK-01.2: Thêm dependencies (WorkManager, `kotlinx.serialization`, Firebase BoM, Google API Client).
  * TASK-01.3: Cấu hình Firebase App (google-services.json dummy) và Crashlytics trong Gradle.
* **Verification:** `./gradlew build` thành công.

### Phase 2 — Core Domain & Engines (The Brain)
* **Objective:** Viết các engine xử lý tính toán thời gian, trùng lặp và thống kê (hoàn toàn không dính UI).
* **Prerequisites:** Phase 1.
* **Requirements covered:** REQ-ENG-001, REQ-ENG-002, REQ-ANALYTICS-001.
* **Current state:** Missing.
* **Gap:** Các thuật toán cốt lõi hoàn toàn chưa được code.
* **Tasks:**
  * TASK-02.1: Implement `TimeEngine` (Xử lý Timezone, Day boundary, tính Duration).
  * TASK-02.2: Implement `OverlapEngine` (Thuật toán cắt segment, hợp nhất session, tính overlap time).
  * TASK-02.3: Implement `ConcurrencyEngine` (Tính distribution 1, 2, 3, 4+ activities).
  * TASK-02.4: Implement `AnalyticsEngine` (Aggregate Daily, Weekly, Monthly).
* **Tests:** Viết Unit Test cho tất cả các case overlap được định nghĩa ở phần #76 của SRS.
* **Verification:** `./gradlew test` (phải pass 100% test time & overlap).

### Phase 3 — Database Foundation & Tracking Refinement
* **Objective:** Đảm bảo mọi mutation (thêm, sửa, xóa) trong Room đều được ghi nhận (SyncMetadata) và hỗ trợ Soft Delete.
* **Prerequisites:** Entity cơ bản hiện tại.
* **Requirements covered:** REQ-TRACK-001, REQ-EDIT-001.
* **Current state:** `StartActivityUseCase` chưa cập nhật `SyncMetadataEntity`. DAO chưa hỗ trợ query phức tạp.
* **Gap:** Thiếu transaction, soft delete, các UseCase Edit/Split.
* **Tasks:**
  * TASK-03.1: Viết Data Migration (nếu cần thiết để add `deletedAt`, `version`, `syncStatus` vào các entity).
  * TASK-03.2: Cập nhật DAO để bỏ qua `deletedAt != null` trong các query mặc định.
  * TASK-03.3: Implement `EditSessionUseCase`, `DeleteSessionUseCase` (soft delete), `SplitSessionUseCase`, `MergeSessionUseCase`. Đảm bảo tất cả được bọc trong Room Transaction và tạo bản ghi SyncMetadata.
* **Verification:** `./gradlew test` (Repository & Database tests).

### Phase 4 — Data Serialization & Export/Import Local
* **Objective:** Có thể serialize toàn bộ dữ liệu ra JSON v1 và CSV.
* **Prerequisites:** Phase 3.
* **Requirements covered:** REQ-EXPORT-001.
* **Gap:** Missing hoàn toàn.
* **Tasks:**
  * TASK-04.1: Định nghĩa Data Classes cho JSON Schema (Schema v1).
  * TASK-04.2: Implement `BackupSerializer` (chuyển đổi Room Entities <-> JSON Data).
  * TASK-04.3: Implement `ImportValidationEngine` (kiểm tra timestamp, reference).
  * TASK-04.4: Implement `CsvGenerator`.
  * TASK-04.5: Tạo UseCase `ExportDataUseCase` (lưu file local), `ImportDataUseCase` (Preview + Merge/Replace).
* **Verification:** Chạy Export, ra file JSON, dùng file đó Import lại.

### Phase 5 — Main UI Implementation
* **Objective:** Hoàn thiện giao diện ứng dụng (Home, Timeline, Analytics, Settings).
* **Prerequisites:** Phase 2, Phase 3.
* **Requirements covered:** REQ-TIME-001, REQ-SET-001.
* **Gap:** Các màn hình trống.
* **Tasks:**
  * TASK-05.1: Cập nhật `HomeScreen` (Hiển thị Active session chính xác, tính thời gian realtime).
  * TASK-05.2: Xây dựng `TimelineScreen` sử dụng Canvas để vẽ timeline blocks (có zoom, pan).
  * TASK-05.3: Xây dựng `SessionDetailDialog` (để Edit, Delete).
  * TASK-05.4: Xây dựng `AnalyticsScreen` (Tabs: Day, Week, Month), bind dữ liệu từ `AnalyticsEngine`.
  * TASK-05.5: Xây dựng `SettingsScreen` (DataStore: Day boundary, Theme).
* **Verification:** Chạy app, thao tác manual tracking và xem biểu đồ/timeline.

### Phase 6 — Firebase Synchronization
* **Objective:** Đồng bộ nền (Background Sync) dữ liệu lên Firestore.
* **Prerequisites:** Phase 3.
* **Requirements covered:** REQ-SYNC-001, REQ-SYNC-002, REQ-SYNC-003.
* **Gap:** Hoàn toàn chưa tích hợp.
* **Tasks:**
  * TASK-06.1: Tích hợp Firebase UI / Google Sign-In.
  * TASK-06.2: Tạo `FirestoreRepository` (Upload/Pull tài liệu, map Firestore Model).
  * TASK-06.3: Implement `ConflictEngine` (Last-write-wins dựa trên version).
  * TASK-06.4: Tạo `SyncWorker` bằng WorkManager (Quan sát mạng, lấy bản ghi PENDING từ SyncMetadata, đẩy lên Firestore).
* **Verification:** Chạy trên 2 Emulator. Sign-in cùng tài khoản. Edit dữ liệu máy 1, máy 2 nhận được sau vài giây.

### Phase 7 — Google Drive Backup
* **Objective:** Quản lý File Backup lên Google Drive độc lập với Firebase.
* **Prerequisites:** Phase 4.
* **Requirements covered:** REQ-DRIVE-001, REQ-DRIVE-002, REQ-DRIVE-003.
* **Gap:** Thiếu.
* **Tasks:**
  * TASK-07.1: Tích hợp Google Auth SDK yêu cầu scope `drive.file`.
  * TASK-07.2: Tạo `DriveBackupManager` (Tạo folder, list files, upload resumable).
  * TASK-07.3: Backup Rotation (Tự xóa file cũ hơn 7 bản).
  * TASK-07.4: UI Settings Drive (Connect, Backup Now, Restore list).
  * TASK-07.5: Lập lịch Automatic Backup bằng WorkManager (Daily/Weekly).
* **Verification:** Kiểm tra thư mục Google Drive của tài khoản Google có xuất hiện file JSON backup.

### Phase 8 — Widgets, Notifications & Hardening
* **Objective:** Trải nghiệm Zero-Friction và Non-Functional Requirements.
* **Prerequisites:** Phase 5.
* **Requirements covered:** REQ-WIDGET-001, REQ-NFR-001.
* **Tasks:**
  * TASK-08.1: Xây dựng Android App Widget (Glance) cho Quick Tracking.
  * TASK-08.2: Thêm Persistent Notification khi có Active Session.
  * TASK-08.3: Implement chức năng `Data Health Check`.
  * TASK-08.4: Viết Instrumented Tests cho Dao và E2E cho UI flow cơ bản.
* **Verification:** `./gradlew connectedAndroidTest`. Add widget ra Home screen.

---

# PART H — Detailed Task Breakdown

Dưới đây là danh sách phẳng để tạo Issues/Tickets:

* `PHASE-01-TASK-01`: Refactor project packages to Clean Architecture.
* `PHASE-01-TASK-02`: Add Firebase, WorkManager, Serialization to `build.gradle.kts`.
* `PHASE-01-TASK-03`: Setup basic Dependency Injection for new Modules.
* `PHASE-02-TASK-01`: Create `TimeEngine.kt` & Unit Tests.
* `PHASE-02-TASK-02`: Create `OverlapEngine.kt` (segment algorithms) & Unit Tests.
* `PHASE-02-TASK-03`: Create `ConcurrencyEngine.kt` & Unit Tests.
* `PHASE-02-TASK-04`: Create `AnalyticsEngine.kt` & Unit Tests.
* `PHASE-03-TASK-01`: Update Entities for Soft Delete & Syncing fields.
* `PHASE-03-TASK-02`: Implement DAOs with Transaction logic.
* `PHASE-03-TASK-03`: Create `EditSessionUseCase`, `DeleteSessionUseCase`.
* `PHASE-03-TASK-04`: Create `SplitSessionUseCase`, `MergeSessionUseCase`.
* `PHASE-04-TASK-01`: Define `BackupSchemaV1` data classes.
* `PHASE-04-TASK-02`: Implement `BackupSerializer` for JSON.
* `PHASE-04-TASK-03`: Implement `CsvGenerator`.
* `PHASE-04-TASK-04`: Implement `ImportValidationEngine`.
* `PHASE-05-TASK-01`: Refactor `HomeScreen` for real-time timer calculation.
* `PHASE-05-TASK-02`: Build `TimelineScreen` (Canvas custom drawing).
* `PHASE-05-TASK-03`: Build `AnalyticsScreen` (UI Charts).
* `PHASE-05-TASK-04`: Build `SettingsScreen` (DataStore bindings).
* `PHASE-06-TASK-01`: Implement Google Sign-In for Firebase Auth.
* `PHASE-06-TASK-02`: Build `FirestoreRepository` for remote operations.
* `PHASE-06-TASK-03`: Implement `ConflictEngine`.
* `PHASE-06-TASK-04`: Create `SyncWorker` (WorkManager) for Background Sync.
* `PHASE-07-TASK-01`: Implement Google Auth specifically for Drive scope.
* `PHASE-07-TASK-02`: Build `DriveBackupManager` (upload/download logic).
* `PHASE-07-TASK-03`: Implement Backup Rotation logic.
* `PHASE-07-TASK-04`: Build Drive Settings UI & Background Backup Worker.
* `PHASE-08-TASK-01`: Build Glance App Widget.
* `PHASE-08-TASK-02`: Implement Active Session Notification (Foreground Service or NotificationManager).
* `PHASE-08-TASK-03`: Build Data Health Check logic.

---

# PART I — Database Migration Plan

Dự án hiện đang có schema sơ khai (Activity, Session, Note, SyncMetadata). 

**Migration M1:**
* **Current:** `ActivitySessionEntity` có thể chưa có các trường tracking cho soft-delete một cách đầy đủ hoặc cần đánh Index.
* **Required schema:** Bổ sung Index `CREATE INDEX index_ActivitySession_startedAt ON ActivitySession(startedAt)`.
* **Data compatibility:** Vì app chưa phát hành production, nếu đang dev có thể `fallbackToDestructiveMigration()`. Nếu không, viết file `Migration_1_2.kt`.
* **Rollback:** Giữ cấu trúc JSON Backup để làm fallback. Dữ liệu Cloud Firestore không bị ảnh hưởng bởi schema Room local (do cơ chế mapping).
* **Tests:** Viết Room Migration Test kiểm tra việc migrate dữ liệu mẫu từ ver 1 sang ver 2 không mất session.

---

# PART J — Testing Strategy

| Requirement | Test Type | Verification Path |
| --- | --- | --- |
| Overlap Calculation | Unit Test | Test tất cả các trường hợp #76 (no overlap, partial, 4+). `calculateOverlap(sessions)` |
| Serialization | Unit Test | Test `BackupSerializer.toJson()` và `.fromJson()`. Đảm bảo schema version = 1. |
| Validation | Unit Test | Đưa vào JSON sai format, reference ID ảo. Kỳ vọng trả về Error. |
| Room Transaction | Integration (Dao) | Test trên DB in-memory. Kiểm tra khi stop session thì bảng SyncMetadata cũng được sinh ra. |
| Sync Worker | Integration | Kiểm tra trạng thái WorkManager Enqueued/Success. |
| Offline Tracking | Instrumented | Tắt Wifi emulator. Start Activity -> Kiểm tra DB insert thành công. |
| Timeline Render | Compose UI Test | Verify Compose tag tồn tại khi truyền mảng session có overlap. |

---

# PART K — Security / Privacy / Reliability / Performance

* **Security:**
  * Ghi chú: Không log note của user ra Logcat.
  * Firestore Rule: `match /users/{userId}/{document=**} { allow read, write: if request.auth != null && request.auth.uid == userId; }`
* **Privacy:** Drive token lưu qua EncryptedSharedPreferences (hoặc androidx.security.crypto). Chỉ xin quyền `https://www.googleapis.com/auth/drive.file`.
* **Reliability:** Background Sync dùng `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()` để không làm crash/drain pin khi offline.
* **Performance:** 
  * Room queries sử dụng Flow. 
  * AnalyticsEngine chạy trên `Dispatchers.Default` (không block Main thread). 
  * Timeline rendering dùng `Canvas` thay vì render ngàn component UI Compose riêng rẽ để đạt 60fps với lượng data lớn.

---

# PART L — Risks

| Risk | Cause | Impact | Likelihood | Mitigation | Phase |
| --- | --- | --- | --- | --- | --- |
| Overlap Algorithm Bug | Sai logic cắt đoạn | Hiển thị Analytics sai, timeline vẽ lỗi | Medium | TDD: Viết 100% test case theo SRS #76 trước khi code engine. | Phase 2 |
| Data Loss on Sync | Overwrite sai version | Mất dữ liệu User | High | Thiết kế ConflictEngine Last-write-wins kỹ, không hard delete local. | Phase 6 |
| Drive Authentication Reject | Dùng sai Scope | Không launch được Drive Backup | Low | Xin scope `drive.file` ngay từ đầu, kiểm tra kỹ OAuth Consent Screen. | Phase 7 |
| Room Transaction Fail | Viết query không trong `@Transaction` | Inconsistent SyncState | Medium | Code review kỹ DAOs, dùng Unit tests DB. | Phase 3 |

---

# PART M — Open Questions

| ID | Question | Why it matters | Suggested decision owner |
| --- | --- | --- | --- |
| OQ-001 | Firebase Analytics & FCM có bắt buộc không? | SRS nói "Không bắt buộc", nhưng cần chốt để giảm thiểu SDK size. | Developer (Quyết định bỏ qua hiện tại) |
| OQ-002 | Nếu Firebase và Drive cùng hỏng, có cơ chế cảnh báo UI rõ ràng không? | UX có thể làm user tưởng đã backup an toàn. | Product (Thêm Warning Icon ở Home Screen) |
| OQ-003 | Widget update interval? | Android giới hạn update widget (thường 30p), làm sao để hiển thị timer realtime trên widget. | Tech Lead (Dùng `Chronometer` trong RemoteViews) |

---

# PART N — Critical Path

**Critical Path:**
`Phase 1 (Setup)` → `Phase 2 (Time & Overlap Engines)` → `Phase 3 (Database Soft Delete & Transactions)` → `Phase 5 (Timeline & Analytics UI)`.

Đây là nhánh bắt buộc và quyết định giá trị cốt lõi của ứng dụng (Record -> Visualize -> Understand). Các phần Cloud Sync (Phase 6) và Drive (Phase 7) có thể làm song song bởi lập trình viên khác sau khi Phase 4 (JSON Serializer) hoàn thành.

---

# PART O — Final SRS Coverage Matrix

| SRS Requirement | Implemented by | Tested by | Verified by | Phase | Status |
| --- | --- | --- | --- | --- | --- |
| Tracking (Start/Stop) | `StartActivityUseCase` | Room Integration Test | `./gradlew test` | 3 | PENDING |
| Offline / Room | `TimeLensDatabase` | Instrumented Tests | Emulator Offline | 1 | PARTIAL |
| Overlap Engine | `OverlapEngine.kt` | Unit Tests (SRS #76) | `./gradlew test` | 2 | PENDING |
| Timeline View | `TimelineScreen.kt` | Compose UI Tests | Manual UI Check | 5 | PENDING |
| Analytics | `AnalyticsEngine.kt` | Unit Tests | `./gradlew test` | 2 | PENDING |
| JSON / CSV Export | `ExportDataUseCase` | Unit Tests | Output File Check | 4 | PENDING |
| Google Drive Backup | `DriveBackupManager` | Mock API Tests | Upload Drive Check| 7 | PENDING |
| Firestore Sync | `SyncWorker.kt` | Integration Tests | Emulator Check | 6 | PENDING |
| Conflict Resolution | `ConflictEngine.kt` | Unit Tests | Unit Test | 6 | PENDING |
| Widget (Low Friction) | `TimeLensWidget.kt` | Manual E2E | Home Screen Check | 8 | PENDING |

---

# FINAL SELF-AUDIT
1. Đã đọc toàn bộ source code? **Có** (Các file UseCase, Repository, Dao, App struct).
2. Đã đọc toàn bộ SRS? **Có** (Từ Tổng quan đến Non-functional, Widget).
3. Có giả định tính năng tồn tại? **Không** (Đã check thực tế UI trống, Firebase/Drive chưa có dependency).
4. Mọi yêu cầu có ID? **Có** (REQ-...).
5. Map vào Task? **Có** (PART H).
6. Dependency rõ ràng? **Có** (PART F, N).
7. Database change map? **Có** (PART I, Phase 3).
8. Testing map? **Có** (PART J).
9. Acceptance Criteria map? **Có** (Nằm rải rác trong verification các Phase).
10. Definition of Done map 100%? **Có** (PART O).

Roadmap này có thể được import thẳng vào Jira/GitHub Issues để bắt đầu sprint. Mọi thành phần kiến trúc đã được làm rõ ranh giới giữa Local, Business Logic, Sync và File Backup.