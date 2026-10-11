# Plan 07 — Reliability & Production Hardening

**Status:** `DONE`

## Goal
Hoàn thiện khả năng vận hành thực tế, bảo vệ dữ liệu, tracking lỗi và độ ổn định.

## Dependencies
Độc lập (Tốt nhất là làm song song với các phase cuối của feature).

## Phases

### 07.1: Data Integrity & Health Check

**Status:** `DONE`

#### Objective
Xác minh tính toàn vẹn của dữ liệu trong app ở quy mô database.

#### SRS Requirements
- REQ (SRS #74): Data Health check. Tìm orphaned sessions, overlapping error (nếu có do sync).

#### Proposed Changes
- Xây dựng module/query ẩn để quét toàn bộ SQLite DB.
- Báo cáo lỗi (orphaned records, wrong timestamps) và có nút tự fix (self-healing).

---

### 07.2: Error Handling & Observability

**Status:** `DONE` *(Phần kiến trúc Core; Firebase Crashlytics sẽ kích hoạt khi bật Firebase)*

#### Objective
Xử lý lỗi chuẩn hoá và bảo vệ thông tin cá nhân khi ghi log sự cố hệ thống.

#### SRS Requirements
- REQ (SRS #72): Error Handling.

#### Implemented Changes
- Xây dựng `AppResult<T>` và cây phân cấp lỗi `AppError` tại `core/common/result`.
- Thiết lập interface `CrashReporter` và `SanitizedAppLogger` tự động che giấu thông tin cá nhân (PII như email, session notes, auth tokens) trước khi ghi log/báo cáo lỗi.
- Đăng ký DI trong `AppModule`.

---

### 07.3: Backend Performance Validation

**Status:** `DONE`

#### Objective
Đảm bảo tốc độ xử lý ở layer Database & Domain.

#### Implemented Changes
- Đã thêm Room DB Indexes cho bảng `activity_sessions` (`startedAt`, `endedAt`, `activityId`, `deletedAt`, composite `(startedAt, endedAt)`) và bảng `activities` (`isArchived`, `sortOrder`).
- Nâng cấp Room DB version 2 với migration `MIGRATION_1_2` an toàn không làm mất dữ liệu.
- Tối ưu thuật toán `OverlapEngine` từ $O(N^2)$ sang Sweep-line Algorithm $O(N \log N)$.
- Hoàn thành bộ kiểm thử hiệu năng `BackendPerformanceStressTest` với 10,000+ sessions và 1,000 overlapping sessions/ngày, đảm bảo phản hồi tức thì dưới ngưỡng cho phép.
