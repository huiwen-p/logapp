# UI Audit & Design System Foundation Report

## 1. Mục tiêu Audit
Đánh giá toàn diện hiện trạng giao diện người dùng (UI/UX) trên toàn bộ ứng dụng TimeLens trước khi tối ưu hóa chuyên sâu theo SRS và Plan 08.
Triết lý thiết kế: **"Zero-friction, trực quan, đáng tin cậy, tôn trọng tính thực tế (Record reality -> Visualize reality -> Understand reality)"**.

---

## 2. Kết quả UI Audit các màn hình hiện tại

| Thành phần / Màn hình | Hiện trạng trước Audit | Vấn đề phát hiện | Giải pháp chuẩn hoá |
| --------------------- | ---------------------- | ---------------- | ------------------- |
| **Typography** | Sử dụng rải rác `displayMedium`, `titleLarge`, `labelLarge` kèm ghi đè `fontWeight = FontWeight.Bold` thủ công trên từng Text component. | Thiếu phân cấp heading/body rõ ràng, kích thước chữ không đồng đều giữa các màn hình. | Chuẩn hóa Material 3 Typography trong `Type.kt` kết hợp `LogAppSectionHeader`. |
| **Spacing & Paddings** | Hardcode trực tiếp các giá trị `4.dp`, `8.dp`, `12.dp`, `16.dp`, `20.dp`, `24.dp`, `32.dp`. | Không có chuẩn token spacing, khoảng cách giữa các khối lộn xộn (lúc 10dp, lúc 12dp, lúc 16dp). | Thiết lập `Spacing` token (`none`, `extraSmall`, `small`, `medium`, `large`, `extraLarge`, `huge`) qua `LocalSpacing`. |
| **Color System & Theming** | Màu chủ đạo `0xFF295de5`. Sử dụng mã màu hex trực tiếp (`#9E9E9E`, `0xFF4CAF50`, `0xFFFF9800`, `0xFFF44336`) cho trạng thái. | Không tương thích mượt mà giữa Light Mode và Dark Mode. Độ tương phản (contrast) một số badge chưa đạt chuẩn M3. | Xây dựng `SemanticColors` (`LightSemanticColors`, `DarkSemanticColors`) với bộ cặp container/onContainer cho `Success`, `Warning`, `Info`, `Error`. |
| **Button Hierarchy** | Nút bấm chỉ có `Button` mặc định và `OutlinedButton`, thiếu phân cấp hành động. | Nút nguy hiểm (xóa/hủy) trông giống nút hành động thông thường hoặc phải style thủ công. Thiếu spinner loading khi đang xử lý tác vụ bất đồng bộ. | Xây dựng bộ `LogAppButton`: `PrimaryButton`, `SecondaryButton`, `TonalButton`, `DangerButton`, `TextButton` có sẵn `isLoading` và `icon`. |
| **Cards & Containers** | Các màn hình tự định nghĩa `Card`, `Surface`, `OutlinedCard` với các bán kính góc khác nhau (`12.dp`, `16.dp`, `20.dp`). | Giao diện thiếu tính nhất quán thị giác (visual consistency). | Thống nhất `LogAppCard`, `LogAppElevatedCard`, `LogAppOutlinedCard` với bo góc chuẩn `16.dp` và padding `spacing.medium`. |
| **Status Badges** | Dùng icon chấm tròn `●` hoặc text `✓`, `⚠` tự nối chuỗi thủ công. | Thiếu tính chuyên nghiệp, không có nền chip nhận diện trạng thái nhanh. | Thêm `LogAppStatusBadge` với các mức `SUCCESS`, `WARNING`, `ERROR`, `INFO`, `NEUTRAL`, `ACTIVE`. |
| **Dialogs & Bottom Sheets** | Code boilerplate `AlertDialog` lặp lại nhiều nơi; header của Bottom Sheet mỗi màn hình tự vẽ một kiểu. | Trải nghiệm xác nhận (Confirm/Dismiss) không đồng nhất. | Thống nhất `LogAppConfirmDialog` và `LogAppBottomSheetHeader`. |

---

## 3. Hệ thống Design System Tokens đã thiết lập

### 3.1. Spacing Tokens (`core/designsystem/tokens/Spacing.kt`)
- `none = 0.dp`
- `extraSmall = 4.dp`
- `small = 8.dp`
- `medium = 16.dp`
- `large = 24.dp`
- `extraLarge = 32.dp`
- `huge = 48.dp`

### 3.2. Semantic Colors (`core/designsystem/tokens/ColorTokens.kt`)
Hỗ trợ đầy đủ Light/Dark Theme:
- **Success:** Dùng cho phiên hoàn thành, dữ liệu Healthy, sao lưu thành công.
- **Warning:** Dùng cho phiên xung đột, phát hiện lỗi dữ liệu cần khắc phục, cảnh báo dung lượng.
- **Error:** Dùng cho lỗi hệ thống, thao tác xóa dữ liệu vĩnh viễn.
- **Info:** Dùng cho thông tin hướng dẫn, tính năng Day Boundary, sync metadata.
- **Active / Primary:** Dùng cho phiên đang bấm giờ chạy theo thời gian thực.

### 3.3. Component Suite (`core/designsystem/component/`)
1. **Buttons:** `LogAppPrimaryButton`, `LogAppSecondaryButton`, `LogAppTonalButton`, `LogAppDangerButton`, `LogAppTextButton`.
2. **Cards:** `LogAppCard`, `LogAppElevatedCard`, `LogAppOutlinedCard`, `LogAppSectionHeader`.
3. **Badges:** `LogAppStatusBadge` với `StatusType`.
4. **Dialogs:** `LogAppConfirmDialog` với tùy chọn `isDestructive`.
5. **Bottom Sheets:** `LogAppBottomSheetHeader`.
6. **Feedback:** `LogAppSnackbarHost`.
