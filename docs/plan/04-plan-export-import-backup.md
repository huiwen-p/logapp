# Plan 04 — Export, Import & Local Backup

**Status:** `TODO`

## Goal
Xây dựng cơ chế Export (JSON, CSV) và Import an toàn.

## Dependencies
Plan 01

## Phases

### Phase 1: CSV Generator & Export UI

#### Objective
Tạo tính năng xuất CSV và kết nối UI cho thao tác Export.

#### SRS Requirements
- REQ: Xuất dữ liệu ra JSON và CSV.
- REQ (SRS #87): `BackupRepository` trả về `java.io.File`.

#### Current Evidence
- Code hiện tại có khai báo Interface cho `createJsonBackup(): File`.
- `BackupSerializerImpl` đã thực hiện Serialize JSON.

#### Proposed Changes
- Implement `exportCsv()` cho `BackupRepository`.
- Gọi hệ thống chia sẻ file của Android để người dùng lấy file File (hoặc sử dụng `Uri`/`Intent.ACTION_CREATE_DOCUMENT` theo chuẩn Scoped Storage). Mặc dù SRS nói `File`, ở tầng UI nên xử lý cẩn thận `FileProvider` nếu cần chia sẻ ra ngoài.

---

### Phase 2: JSON Validation & Import UI

#### Objective
Kiểm tra cấu trúc JSON trước khi import.

#### SRS Requirements
- REQ: Restore Mode có Merge và Replace (SRS #40).

#### Proposed Changes
- Validate phiên bản Schema.
- Viết `ImportDataUseCase`.
- Tạo UI `ImportScreen` (Preview dữ liệu, Hỏi Merge / Replace).

#### Tests
- `Unit Test`: `ImportValidationEngine` từ chối file có ID Activity không tồn tại (nếu reference).
- `Unit Test`: Merge Mode và Replace Mode (Deduplicate rule).
