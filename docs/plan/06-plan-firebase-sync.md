# Plan 06 — Firebase Cloud Sync

**Status:** `BLOCKED`

## Rationale for Block
Kiến trúc Sync hiện tại chưa có đặc tả "Sync Protocol" và "Conflict Resolution Model" rõ ràng từ hệ thống (VD: dùng version so sánh, hay dùng updatedAt, hay server-wins, hay CRDTs). Nếu AI/Developer tự ý cài đặt một hệ thống, có nguy cơ rất cao dẫn đến data corruption, lost updates khi 2 thiết bị thay đổi đồng thời.

SRS yêu cầu Soft Delete (#50) và Sync Flow cơ bản (#47), nhưng chưa đủ chi tiết về conflict resolution. KHÔNG được code trước khi có bản thiết kế kỹ thuật.

## Phase 0: Sync Protocol Design (Required)
- Xác định cách xác thực version hoặc timestamp (VD: LWW - Last Write Wins).
- Thiết kế Data Schema trên Firestore.
- Liệu có thực sự cần thiết "Realtime Listeners" không, hay định kỳ WorkManager (Periodic) là đủ?

## Expected Phases (Sau khi Unblocked)

### Phase 1: Mở Rộng Firestore Repository
- Upload/Pull hỗ trợ tất cả Entity.

### Phase 2: Pull & Reconciliation Engine
- Kéo từ Firebase về, giải quyết xung đột (conflict resolution), update Local, Push trở lại.

### Phase 3: Xử lý trạng thái SYNC_PENDING
- Đánh dấu các bản ghi trong Room chưa đồng bộ, sau đó WorkManager lo việc upload.
