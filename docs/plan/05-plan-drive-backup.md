# Plan 05 — Google Drive Backup & Restore

**Status:** `DONE`

## Goal
Tính năng Auto Backup/Restore lên Google Drive thành một file JSON.

## Dependencies
Plan 04 (Cần JSON schema export)

## Phases

### Phase 1: Download / Restore Drive file `DONE`

#### Objective
Tải file JSON từ Google Drive xuống và truyền vào hệ thống Import.

#### Current Evidence
- `DriveBackupManagerImpl` đã có hàm upload (chỉ trả về ID file) nhưng chưa có hàm download.

#### Proposed Changes
- Thêm `downloadBackup` sử dụng Google Drive API.
- UI: Hiển thị list file backup trên Drive, chọn và tải về.

---

### Phase 2: Automatic Backup Scheduler `DONE`

#### Objective
WorkManager chạy ngầm tự động backup định kỳ và xoay vòng file (retention rotation).

#### SRS Requirements
- REQ (SRS #37): Automatic Drive Backup, Daily/Weekly/Manual.
- REQ (SRS #38): Backup Retention Rotation, "Latest 7 backups" default.

#### Completed Changes
- WorkManager cấu hình `DriveBackupWorker` qua Hilt `HiltWorkerFactory` với unmetered network và battery constraints.
- `DriveBackupScheduler` hỗ trợ lập lịch chu kỳ Periodic Work (Daily 1 ngày, Weekly 7 ngày, Manual hủy).
- Upload file JSON lên Google Drive, sau đó tự động gọi `deleteOldBackups` xoay vòng giữ số bản backup cấu hình (mặc định 7 bản).
- Settings UI: Bật/tắt Auto Backup, chọn tần suất (Hàng ngày / Hàng tuần / Thủ công), cấu hình số lượng lưu trữ (7, 30, 1, Tất cả).
