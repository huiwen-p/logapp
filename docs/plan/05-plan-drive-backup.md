# Plan 05 — Google Drive Backup & Restore

**Status:** `TODO`

## Goal
Tính năng Auto Backup/Restore lên Google Drive thành một file JSON.

## Dependencies
Plan 04 (Cần JSON schema export)

## Phases

### Phase 1: Download / Restore Drive file

#### Objective
Tải file JSON từ Google Drive xuống và truyền vào hệ thống Import.

#### Current Evidence
- `DriveBackupManagerImpl` đã có hàm upload (chỉ trả về ID file) nhưng chưa có hàm download.

#### Proposed Changes
- Thêm `downloadBackup` sử dụng Google Drive API.
- UI: Hiển thị list file backup trên Drive, chọn và tải về.

---

### Phase 2: Automatic Backup Scheduler

#### Objective
WorkManager chạy ngầm tự động backup định kỳ.

#### SRS Requirements
- REQ (SRS #37): Automatic Drive Backup, "Latest 7 backups".

#### Proposed Changes
- WorkManager chạy daily.
- Upload file, sau đó lấy danh sách file và xóa cũ nếu vượt quá 7 file.
- Setting UI: Bật/tắt Auto Backup.
