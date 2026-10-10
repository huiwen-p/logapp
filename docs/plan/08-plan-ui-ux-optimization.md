# Plan 08 — UI/UX Development & Optimization

**Status:** `TODO`

## Goal
Phát triển UI/UX như một "trụ cột" song song với Data/Logic. Biến ứng dụng từ "chạy được" sang "zero-friction, dễ hiểu, đáng tin cậy".

## Dependencies
Các phase của Plan này có thể và nên được thực thi **song song** ngay sau khi core feature logic của các Plan 01-06 được hoàn thiện. 

## Phases

### Phase 1: UI Audit & Design System Foundation
**Objective:** Chuẩn hoá thiết kế trước khi code giao diện.
- **Audit:** Kiểm tra typography, spacing, colors, iconography hiện tại trên tất cả màn hình (Home, Timeline, Analytics).
- **Design System:** Quy chuẩn Color system, Button hierarchy, Cards, Bottom Sheets, Dialogs (cảnh báo xóa/merge), Snackbar. Không được mỗi nơi thiết kế một kiểu.
- **Theming:** Hỗ trợ đầy đủ Dark/Light mode và Contrast chuẩn Material 3.

### Phase 2: Home / Tracking UX
**Objective:** Thao tác tracking (Start/Stop) gần như zero-friction.
- Tối ưu luồng "Open app -> Chọn Activity -> Start".
- Xử lý các UI edges: Tên activity dài, có hàng chục activities, không có activity nào (Empty State), Activity bị archive.
- Widget Tracking: Mang nút Start/Stop ra màn hình chính, phản hồi lập tức.

### Phase 3: Timeline UX
**Objective:** Khả năng đọc dữ liệu và thao tác (Edit/Split/Merge) trên Timeline mượt mà.
- **Information density:** Khi có 1 session vs 50 sessions/ngày, Timeline vẫn hiển thị rõ.
- Xử lý trực quan việc chọn 2 sessions kề nhau để Merge.
- Hiển thị trực quan (visual cue) khi có Overlap.
- Xử lý UI báo lỗi nếu Split time nằm ngoài start/end.

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
