# Master Roadmap

## Status Categories
- `TODO`: Sẵn sàng để thực thi.
- `IN_PROGRESS`: Đang được thực thi.
- `BLOCKED`: Cần làm rõ requirement hoặc design trước khi thực thi.
- `DONE`: Hoàn thành.

## Current State

Dự án hiện đang ở mức độ Proof of Concept / Early Alpha. 
- **Core Tracking:** Đã có thể Start/Stop activity thông qua Room database. Hỗ trợ overlap và offline.
- **Timeline:** Đã hiển thị dạng Canvas cho một ngày. Chưa hỗ trợ thao tác Split/Merge trên giao diện.
- **Analytics:** Đã có `AnalyticsEngine` để tính overlap và concurrency cho Daily. Chưa có Weekly/Monthly.
- **Backup & Sync:** Đã có cấu trúc interface, `SyncWorker` đã đẩy dữ liệu session lên Firestore nhưng chưa hoàn chỉnh. `DriveBackupManagerImpl` đã upload được JSON.
- **UI/UX:** Mới chỉ có các luồng cơ bản, thiếu Design System, Empty States, Error States và cơ chế phản hồi (feedback mechanism) cho quá trình Sync/Backup.

## Target State

Hoàn thiện toàn bộ tính năng theo đúng SRS:
- Quản lý Activity với lựa chọn Archive (hoặc xóa/gộp).
- Quản lý Session hoàn chỉnh với Add thủ công, Edit, Delete (soft delete), Split, Merge.
- Analytics đa chiều (Daily, Weekly, Monthly) tách biệt.
- Export/Import JSON & CSV chuẩn xác.
- Backup tự động qua Google Drive (giữ 7 bản, có restore).
- Sync thời gian thực qua Firebase.
- Trải nghiệm UI/UX zero-friction, stateful (Loading/Error/Empty) ở mọi luồng thao tác.

## Overall Dependency Graph

```text
                 ┌──────────────→ Plan 02 ───→ Plan 03
                 │
Plan 01 ─────────┼──────────────→ Plan 04 ───→ Plan 05
                 │
                 └──────────────→ Plan 06

(Plan 07 chạy độc lập cho Data/Reliability)
(Plan 08 chạy song song với các Plan feature tương ứng)
```

## Plans

| Order | Plan | Goal | Depends On | Risk | Status |
| ----- | ---- | ---- | ---------- | ---- | ------ |
| 01 | [Plan 01: Hoàn thiện Core Domain](01-plan-hoan-thien-core-domain.md) | Xử lý Delete/Archive Activity, Soft Delete Session, Day Boundary. | Không | LOW | `DONE` |
| 02 | [Plan 02: Timeline & Session Management](02-plan-timeline-session-management.md) | Thêm Split, Merge, Add manual, Edit session. | Plan 01 | MEDIUM | `DONE` |
| 03 | [Plan 03: Analytics & Reports](03-plan-analytics-reports.md) | Hoàn thiện Daily, Weekly, Monthly và Activity Relationships. | Plan 02 | LOW | `DONE` |
| 04 | [Plan 04: Export, Import & Local Backup](04-plan-export-import-backup.md) | Export CSV, Import JSON Validation (Merge/Replace). | Plan 01 | HIGH | `TODO` |
| 05 | [Plan 05: Google Drive Backup & Restore](05-plan-drive-backup.md) | Auto Backup (7 files), Download/Restore. | Plan 04 | MEDIUM | `TODO` |
| 06 | [Plan 06: Firebase Cloud Sync](06-plan-firebase-sync.md) | Đồng bộ 2 chiều (Cloud/Local), Xử lý Conflict. | Plan 01 | CRITICAL | `BLOCKED` |
| 07 | [Plan 07: Reliability & Hardening](07-plan-production-hardening.md) | Data Health, Error Handling, Performance Profiling. | Độc lập | LOW | `TODO` |
| 08 | [Plan 08: UI/UX Development](08-plan-ui-ux-optimization.md) | Design System, Stateful UX, Journey Testing. | Độc lập | MEDIUM | `TODO` |
