# Plan 08 — UI/UX Development & Optimization

**Status:** `IN_PROGRESS`

## Goal
Phát triển UI/UX như một "trụ cột" song song với Data/Logic. Biến ứng dụng từ "chạy được" sang "zero-friction, dễ hiểu, đáng tin cậy".

## Dependencies
Các phase của Plan này có thể và nên được thực thi **song song** ngay sau khi core feature logic của các Plan 01-06 được hoàn thiện. 

## Phases

### Phase 1: UI Audit & Design System Foundation

**Status:** `DONE`

**Objective:** Chuẩn hoá thiết kế trước khi code giao diện.
- **Audit:** Hoàn thành tài liệu UI Audit toàn diện tại [docs/design-system-audit.md](file:///d:/logapp/docs/design-system-audit.md).
- **Design System:** Xây dựng Design System Foundation hoàn chỉnh tại `core/designsystem/`:
  - `tokens/Spacing.kt`: Spacing tokens (`none` đến `huge`) và `LocalSpacing`.
  - `tokens/ColorTokens.kt`: `SemanticColors` (`Success`, `Warning`, `Info`, `Error`) chuẩn Material 3 cho cả Light & Dark mode.
  - `component/LogAppButton.kt`: Phân cấp nút (`Primary`, `Secondary`, `Tonal`, `Danger`, `Text`) có loading & icons.
  - `component/LogAppCard.kt`: `LogAppCard`, `LogAppElevatedCard`, `LogAppOutlinedCard`, `LogAppSectionHeader`.
  - `component/LogAppBadge.kt`: `LogAppStatusBadge` với `StatusType`.
  - `component/LogAppDialog.kt`: `LogAppConfirmDialog`.
  - `component/LogAppBottomSheet.kt`: `LogAppBottomSheetHeader`.
  - `component/LogAppSnackbar.kt`: `LogAppSnackbarHost`.
- **Theming:** Tích hợp `LocalSpacing` và `LocalSemanticColors` vào `LogappTheme` (`core/theme/Theme.kt`). Đã áp dụng mẫu vào `SettingsScreen.kt`.

### Phase 2: Home / Tracking UX

**Status:** `DONE`

**Objective:** Thao tác tracking (Start/Stop) gần như zero-friction.
- **Tối ưu luồng 1-tap Start/Stop:** Chạm 1 chạm vào `ActivityCard` bắt đầu ghi nhận tức thì không form/popup (tuân thủ FR-LF-02).
- **Active Sessions UX:** Hiển thị trực quan các phiên đang chạy với live timer đếm giây `HH:mm:ss`, chấm đỏ pulse nhấp nháy, huy hiệu `StatusType.ACTIVE`, nút Dừng 1 chạm.
- **Hỗ trợ Undo thật sự:** Dừng phiên hiển thị Snackbar có nút "Hoàn tác", khôi phục phiên đang chạy (`endedAt = null`) ngay lập tức nếu lỡ tay bấm nhầm.
- **Xử lý trọn vẹn các UI Edges:**
  - *Tên dài:* `ActivityCard` hỗ trợ hiển thị tối đa 2 dòng (`maxLines = 2`), không vỡ tỉ lệ ô vuông `aspectRatio(1f)`.
  - *Hàng chục activities:* Tự động kích hoạt Quick Search Bar khi có từ 6 hoạt động trở lên, tìm kiếm gõ phím tức thì và có nút xóa nhanh.
  - *Empty State:* Card minh họa sinh động khi chưa có hoạt động nào với 2 nút: "Tạo hoạt động mới" và "Thêm hoạt động mẫu".
  - *Activity bị archive:* Tự động loại bỏ khỏi quick tracking, nhưng các session lịch sử hoặc session đang chạy vẫn hiển thị đầy đủ và dừng an toàn.
  - *Trạng thái đang chạy:* `ActivityCard` trên lưới tự động đổi màu viền sáng và gắn chấm đỏ báo hiệu đang chạy.

### Phase 3: Timeline UX

**Status:** `DONE`

**Objective:** Khả năng đọc dữ liệu và thao tác (Edit/Split/Merge) trên Timeline mượt mà.
- **Information density (Cluster-based Layout):** Triển khai `TimelineLayoutEngine`. Các session độc lập (không overlap) luôn chiếm trọn 100% chiều rộng màn hình. Chỉ những cụm session trùng lấn cục bộ mới chia cột, khắc phục triệt để tình trạng 1 session overlap làm co hẹp toàn bộ các session khác trong ngày.
- **Visual cues cho Overlap:**
  - Trên khối session (`TimelineCanvas`): Dải viền báo hiệu màu hổ phách ở cạnh trái và biểu tượng `⇄` ở góc trên bên phải khi có trùng lấn.
  - Trên thanh tóm tắt ngày (`TimelineScreen`): Hiển thị huy hiệu `LogAppStatusBadge` cảnh báo số phút trùng lấn (`Trùng lặp: X phút`) hoặc báo `Không trùng lặp`.
  - Hiển thị đầy đủ nhãn thời gian `HH:mm - HH:mm` trong block khi chiều cao $\ge 56\text{dp}$.
- **Xử lý trực quan & An toàn khi Merge:**
  - Bật chế độ chọn nhiều bằng cách nhấn giữ (long click). Thanh hướng dẫn thông minh báo bước tiếp theo (chọn thêm phiên thứ 2 liền kề).
  - Kiểm tra điều kiện hợp lệ trước khi gộp (cùng hoạt động, liền kề hoặc trùng lấn).
  - Hộp thoại `LogAppConfirmDialog` hiển thị tóm tắt thời gian gộp mới, ghi chú kết hợp và giải thích rõ lý do nếu 2 phiên không đủ điều kiện gộp.
- **Xử lý UI báo lỗi trực quan khi Split:**
  - Hộp thoại `SplitSessionDialog` kiểm tra thời gian chia theo thời gian thực (`startedAt < splitTime < endedAt`).
  - Hiển thị thông báo lỗi màu đỏ nếu vi phạm khoảng thời gian, vô hiệu hóa nút "Xác nhận chia" cho đến khi thời gian hợp lệ.
  - Hiển thị bản xem trước sau khi chia (Phiên 1 và Phiên 2).
- **Empty State & Design System Suite:**
  - Tích hợp `LogAppCard` rỗng khi ngày chưa có dữ liệu, có nút "Thêm phiên thủ công".
  - Chuẩn hóa toàn bộ `SessionBottomSheet` và `CreateSessionBottomSheet` theo Design System tokens (`LogAppBottomSheetHeader`, `LogAppPrimaryButton`, `LogAppSecondaryButton`, `LogAppDangerButton`, `LogAppConfirmDialog`).

### Phase 4: Analytics UX (Information Hierarchy)
**Objective:** Người dùng đọc bảng phân tích và hiểu kết quả trong vài giây.
- Thiết kế hệ thống tab Day / Week / Month.
- Tóm lược (Summary Card): Hiển thị Unique time, Overlap, Tracked time rõ ràng ngay đầu.
- Cấu trúc "Activity Breakdown" với thanh Progress Bar (ai chiếm nhiều % time nhất).
- Rành mạch giữa "Số liệu thô" và "Phân tích Activity Relationships".

### Phase 5: Activity Management UX
**Objective:** Quản lý vòng đời Activity.
- Tối ưu UX cho Create, Edit (đổi màu/icon), Archive.
- Tìm kiếm (Search) nếu user có >30 activities.
- Thay đổi thứ tự (Reorder) trên list.

### Phase 6: Export / Backup / Sync State UX
**Objective:** Trực quan hóa trạng thái an toàn dữ liệu để người dùng an tâm.
- Màn hình Backup: Ghi rõ "Last backup: Today 02:00" với biểu tượng đám mây. Trạng thái Đang backup, Thành công, Lỗi.
- Màn hình Sync: Icon "Đang đồng bộ", "Offline", "Xung đột dữ liệu". Báo rõ cho user nếu Firebase đẩy lỗi.

### Phase 7: Loading / Error / Empty / Offline States
**Objective:** Mọi màn hình phải có state model đầy đủ (Stateful UI).
- Empty States: Ví dụ "Bạn chưa track gì hôm nay", thay vì màn hình trắng bóc.
- Loading: Skeleton UI hoặc progress indicators khi tính toán Analytics lớn.
- Error/Offline: Nút "Retry" nếu không kết nối được Firebase. 

### Phase 8: UI Performance & Interaction
**Objective:** Ứng dụng phản hồi ngay lập tức, cuộn mượt (60fps).
- Audit Compose Recomposition: Dùng Layout Inspector.
- Test `LazyColumn` với 1,000+ sessions.
- Xử lý mượt các Dialog animation, Sheet expansion.

### Phase 9: Final UX Review (Real User Journeys)
**Objective:** Kiểm thử toàn bộ quá trình sử dụng.
- **Journey 1:** Mở app -> Start -> Stop -> Mở Timeline kiểm tra.
- **Journey 2:** Quên stop -> Sửa Timeline -> Split session -> Check lại Analytics.
- **Journey 3:** Lấy máy mới -> Đăng nhập -> Restore Drive JSON -> Dữ liệu vào đủ.
- Đánh giá theo góc nhìn End-User, không đánh giá theo góc nhìn Engineer.
