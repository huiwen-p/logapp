# Plan 01 — Hoàn thiện Core Domain

**Status:** `DONE`

## Goal
Đảm bảo các Activity và Session tuân thủ chặt chẽ các luật business cốt lõi của hệ thống, chuẩn bị nền tảng an toàn cho Sync và UI.

## Dependencies
Không

## Phases

### Phase 1: Hoàn thiện logic Delete Activity (Archive) - DONE

#### Objective
Áp dụng đúng luật xóa Activity theo SRS: Hỏi người dùng muốn Archive hay xử lý session nếu Activity đã có dữ liệu.

#### SRS Requirements
- REQ: User có quyền archive thay vì hard delete (SRS Line 248-255).
- REQ: Không được xóa Activity nếu làm mất session lịch sử.

#### Current Evidence
- File: `ActivityEntity.kt`
- Hiện trạng: Đã có trường `isArchived`. `DeleteActivityUseCase` có thể chưa xử lý logic chặn/hỏi user này.

#### Proposed Changes
- Khảo sát/Sửa đổi `DeleteActivityUseCase` để kiểm tra có session hay không. Nếu có session -> ném exception hoặc trả kết quả yêu cầu UI hỏi user.
- Thêm `ArchiveActivityUseCase`.

#### Tests
- `Unit Test`: `ArchiveActivityUseCase` set `isArchived = true`.
- `Unit Test`: `DeleteActivityUseCase` khi có session -> throw `ActivityHasSessionsException`.

#### Scope Boundary
- THIS PHASE DOES: Cập nhật usecase và repository cho thao tác xóa/archive.
- THIS PHASE DOES NOT: Thay đổi UI phức tạp hay đụng đến Timeline.

---

### Phase 2: Session Soft Delete - DONE

#### Objective
Cập nhật mọi thao tác xoá Session thành cập nhật `deletedAt` để đảm bảo Cloud Sync có mốc dữ liệu đồng bộ.

#### SRS Requirements
- REQ (SRS #50 - Soft Delete): Session không nên hard delete ngay trong cloud sync. Dùng `deletedAt`.

#### Current Evidence
- File: `ActivitySessionEntity.kt` (Có field `deletedAt`).
- File: `DeleteSessionUseCase.kt` (Đang xoá vật lý hoặc chưa tuân thủ).

#### Proposed Changes
- `SessionDao.kt`: Cập nhật mọi query lấy session đang active phải filter `deletedAt IS NULL`.
- Bổ sung `softDelete` query trong DAO.
- Thay đổi `DeleteSessionUseCase` để gọi `softDelete`.

#### Tests
- `Unit Test`: Soft delete thành công, giá trị `deletedAt` được gán.
- `Integration Test`: Các query Get không trả về session đã soft delete.

---

### Phase 3: Implement Day Boundary Configuration - DONE

#### Objective
Hỗ trợ cài đặt thời gian bắt đầu ngày mới thay vì mặc định `00:00`.

#### SRS Requirements
- REQ (SRS #56 - Day Boundary): Mặc định `00:00`, user có thể cấu hình (vd `04:00`).

#### Current Evidence
- Hệ thống chưa có cấu hình Day Boundary, có thể đang tính mặc định theo calendar (00:00).

#### Proposed Changes
- Tạo/Cập nhật `PreferencesDataStore` lưu `dayBoundary`.
- Sửa các logic trong `AnalyticsEngine` và `TimelineViewModel` để lấy offset dựa trên config này.

#### Tests
- `Unit Test`: Session lúc 02:00 ngày 2/10 thuộc về Timeline ngày 1/10 nếu Boundary là 04:00.
