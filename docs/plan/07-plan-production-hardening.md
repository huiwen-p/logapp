# Plan 07 — Reliability & Production Hardening

**Status:** `TODO`

## Goal
Hoàn thiện khả năng vận hành thực tế, bảo vệ dữ liệu, tracking lỗi và độ ổn định.

## Dependencies
Độc lập (Tốt nhất là làm song song với các phase cuối của feature).

## Phases

### 07.1: Data Integrity & Health Check

#### Objective
Xác minh tính toàn vẹn của dữ liệu trong app ở quy mô database.

#### SRS Requirements
- REQ (SRS #74): Data Health check. Tìm orphaned sessions, overlapping error (nếu có do sync).

#### Proposed Changes
- Xây dựng module/query ẩn để quét toàn bộ SQLite DB.
- Báo cáo lỗi (orphaned records, wrong timestamps) và có nút tự fix (self-healing).

---

### 07.2: Error Handling & Observability

#### Objective
Báo cáo crash và sự cố hệ thống lên Firebase Crashlytics một cách an toàn.

#### SRS Requirements
- REQ (SRS #72): Error Handling.

#### Proposed Changes
- Cấu hình Crashlytics log các non-fatal exception, giấu thông tin cá nhân (như "note text").
- Đặt `try-catch` chuẩn ở Repository layer. Mapping exception -> Result domain model.

---

### 07.3: Backend Performance Validation

#### Objective
Đảm bảo tốc độ xử lý ở layer Database & Domain.

#### Proposed Changes
- Chạy stress test sinh ra lượng lớn session (10,000+) và đo tốc độ của `AnalyticsEngine`, `OverlapEngine`.
- Thêm DB Index cho Room nếu các query (đặc biệt là theo khoảng thời gian `startedAt`) bị chậm.
