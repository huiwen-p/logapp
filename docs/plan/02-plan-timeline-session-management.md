# Plan 02 — Timeline & Session Management

**Status:** `TODO`

## Goal
Mang lại trải nghiệm quản lý Session đầy đủ trên giao diện Timeline: Add Manual, Split, Merge, Edit.

## Dependencies
Plan 01

## Phases

### Phase 1: Manual Add Session & Edit Session (DONE)

#### Objective
Cho phép người dùng tạo/sửa một session trong quá khứ.

#### SRS Requirements
- Hỗ trợ thao tác thêm/sửa thủ công.

#### Proposed Changes
- Tạo `CreateManualSessionUseCase` / `EditSessionUseCase` với validate: `end >= start`, không duration âm.
- Mở rộng giao diện `SessionBottomSheet` để cho phép chọn giờ thủ công.

#### Tests
- `Unit Test`: `CreateManualSessionUseCase` chặn `end < start`.

---

### Phase 2: Split Session (DONE)

#### Objective
Logic chia 1 session thành 2.

#### SRS Requirements
- REQ: Cho phép Split (SRS).

#### Current Evidence
- Chưa có UI hay logic cụ thể trong `TimelineScreen`.

#### Proposed Changes
- `SplitSessionUseCase(sessionId, splitTime)` -> Tạo sessionA và sessionB.
- Validate: `splitTime` nằm giữa `start` và `end`.

#### Tests
- `Unit Test`: Split thành công, 2 session mới có tổng thời gian bằng session cũ, ID cũ bị soft delete (hoặc update 1, tạo 1).

---

### Phase 3: Define & Implement Merge Session (DONE)

**Status:** `DONE`

#### Objective
Gộp hai session của cùng một Activity.

#### SRS Requirements
- REQ (SRS): "Hai session cùng Activity có thể được merge thủ công."

#### Gap / Open Questions
- SRS chưa làm rõ:
  1. Có cho phép merge hai session không liền kề nhau không (có gap)?
  2. Nếu có gap, phần gap có được tính thành thời gian hoạt động (bị lấp đầy) hay bị bỏ qua?
- **Action Required:** Yêu cầu người dùng (hoặc audit kĩ lại văn bản SRS) để xác nhận luật trước khi code.

#### Scope Boundary
- KHI ĐÃ CÓ RULE: Cập nhật `MergeSessionUseCase` theo đúng rule. Cập nhật `TimelineScreen` có mode đa chọn (Select multiple) để merge.
