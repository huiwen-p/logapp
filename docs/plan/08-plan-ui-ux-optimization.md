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

**Status:** `DONE`

**Objective:** Người dùng đọc bảng phân tích và hiểu kết quả trong vài giây.
- **Hệ thống Tab & Điều hướng Thời gian (Day / Week / Month Navigation):**
  - Hỗ trợ đầy đủ bộ điều hướng thời gian đa chiều: `< Hôm nay (11/10) >`, `< Tuần dd/MM - dd/MM >`, `< Tháng MM/yyyy >`.
  - Nút bấm quay về kỳ hiện tại tức thì (`resetToCurrentPeriod()`).
  - Hỗ trợ chuyển đổi mượt mà giữa các tab Ngày, Tuần, Tháng.
- **Tóm lược Đỉnh cao (Time Summary Hero Cards):**
  - Bộ 3 chỉ số cốt lõi được trực quan hóa ngay đầu màn hình: **Thời gian thực (Unique Clock Time)**, **Tổng ghi nhận (Tracked Time)**, **Thời gian Trùng lặp (Overlap Time)**.
  - Biểu đồ vành khuyên [RingChart] kết hợp huy hiệu [LogAppStatusBadge] phân loại rõ ràng trạng thái "Đơn nhiệm" vs "Đa nhiệm".
  - Hiển thị chỉ số trung bình ngày (`Avg/ngày`) cho kỳ Tuần và Tháng.
- **Phân bổ Mức độ Đồng thời (Concurrency Distribution):**
  - Tách bạch rõ rệt giữa thời gian làm việc đơn nhiệm (Mức 1) và đa nhiệm (Mức 2+).
- **Phân bổ Hoạt động (Activity Breakdown) chuẩn chỉnh:**
  - Sắp xếp giảm dần theo thời lượng; hiển thị tỷ lệ phần trăm chính xác (`%`), tên hoạt động, thời gian thân thiện (`Xh Yp`).
  - Thanh tiến trình `LinearProgressIndicator` mang màu sắc đặc trưng riêng của từng Activity.
- **Tách bạch Mối quan hệ Hoạt động (Activity Relationships):**
  - Khu vực riêng biệt phân tích sâu hiện tượng làm việc song song giữa các cặp hoạt động (`A + B`) kèm tỷ lệ đóng góp trong tổng thời gian overlap.
- **Xử lý Empty State trang nhã:**
  - Tự động hiển thị `AnalyticsEmptyStateCard` khi một ngày/tuần/tháng chưa có dữ liệu ghi nhận.

### Phase 5: Activity Management UX

**Status:** `DONE`

**Objective:** Quản lý vòng đời Activity toàn diện, trực quan và an toàn dữ liệu.
- **Vòng đời Activity (Create, Edit, Archive, Safe Delete):**
  - **Tạo & Chỉnh sửa Hoạt động (`ActivityFormBottomSheet`):**
    - Hỗ trợ thẻ xem trước trực quan (`LiveActivityPreview`) cập nhật thời gian thực màu sắc, biểu tượng và tên hiển thị.
    - Kiểm tra tính hợp lệ tên hoạt động trong thời gian thực: cảnh báo lỗi khi tên rỗng hoặc trùng lặp với hoạt động khác (case-insensitive).
    - Chuẩn hóa toàn bộ nút hành động theo Design System: [LogAppPrimaryButton], [LogAppSecondaryButton], [LogAppDangerButton].
  - **Quy tắc Lưu trữ (Archive) & Bảo toàn dữ liệu theo SRS:**
    - Lưu trữ (Archive) hoạt động: Ẩn khỏi màn hình chính và Quick Tracking, bảo toàn 100% lịch sử trong Nhật ký (Timeline) và Thống kê (Analytics).
    - Khôi phục (Unarchive) hoạt động: Đưa hoạt động trở lại danh sách theo dõi nhanh chỉ với 1 thao tác.
    - Hộp thoại xác nhận an toàn [LogAppConfirmDialog] với thông điệp rõ ràng, giải thích tác động khi Lưu trữ và Khôi phục.
    - **Xóa cứng (Hard Delete) an toàn:** Tích hợp [DeleteActivityUseCase] và kiểm tra số lượng session trong lịch sử. Nếu hoạt động đã có session $\ge 1$, hệ thống ngăn chặn xóa cứng và chủ động hướng dẫn người dùng chuyển sang Lưu trữ (Archive). Nếu chưa có session nào, cho phép xóa vĩnh viễn sau khi xác nhận qua dialog cảnh báo màu đỏ.
- **Tìm kiếm thời gian thực (Real-time Search):**
  - Thanh tìm kiếm thông minh (`OutlinedTextField`) hỗ trợ lọc tức thì theo tên hoạt động khi người dùng có nhiều hoạt động (>10-30 hoạt động).
  - Nút xóa bộ lọc nhanh (`Clear trailing icon`) và chuyển đổi mượt mà giữa các tab.
  - Card giao diện rỗng chuyên dụng (`EmptySearchResultCard`) khi không có kết quả khớp với từ khóa tìm kiếm.
- **Thay đổi thứ tự (Reorderable List) với kéo thả mượt mà:**
  - Tích hợp thư viện `reorderable` với biểu tượng tay nắm kéo trực quan (`Icons.Default.Menu`) cho từng hoạt động ở tab "Đang theo dõi".
  - Hiệu ứng đổ bóng nâng cao (`elevation = 8.dp`) và tô màu nổi bật khi người dùng đang kéo thả item.
  - Banner hướng dẫn tinh tế: "Nhấn giữ biểu tượng ☰ để kéo thả sắp xếp thứ tự hiển thị".
  - Tự động tạm tắt chế độ kéo thả khi đang tìm kiếm để tránh sai lệch chỉ mục hiển thị.
  - Tự động cập nhật `sortOrder` xuống cơ sở dữ liệu Room ngay khi kết thúc thao tác kéo.
- **Hệ thống Màu sắc & Biểu tượng Tuyển chọn (`ActivityPresets`):**
  - Bảng màu 16 sắc thái Material 3 / pastel rực rỡ, trang nhã, tương phản tốt trên cả giao diện Sáng (Light) và Tối (Dark).
  - Bộ 18 biểu tượng / emoji thân thiện đại diện cho các lĩnh vực đời sống (Lập trình, Công việc, Sách/Học tập, Gym/Thể thao, Game, Cà phê, Ngủ, v.v.) kèm tùy chọn chữ cái đầu.
  - Hỗ trợ hiển thị icon/emoji đồng bộ trên cả [ActivityCard] và [ActiveSessionItem] ở màn hình chính.
- **Phân tách Tab & Huy hiệu Đếm số lượng:**
  - Tab "Đang theo dõi" và "Đã lưu trữ" đi kèm huy hiệu [LogAppStatusBadge] thể hiện số lượng chính xác theo thời gian thực.
  - Xử lý trạng thái rỗng (`EmptyActiveActivitiesCard`, `EmptyArchivedActivitiesCard`) giải thích rõ ràng và có nút tạo mới ngay tức thì.
- **Bộ Unit Tests hoàn chỉnh (`ManageActivitiesViewModelTest`):**
  - Kiểm thử phân tách active vs archived, tìm kiếm thời gian thực, validation tên (trùng/rỗng), tạo mới, cập nhật, reorder thứ tự, chặn xóa khi có session, và xóa khi 0 session.

### Phase 6: Export / Backup / Sync State UX

**Status:** `DONE`

**Objective:** Trực quan hóa trạng thái an toàn dữ liệu theo triết lý Offline-first, sẵn sàng kết nối đám mây trong tương lai (kiến trúc mở, không đi vào đường cụt).
- **Trực quan hóa Trạng thái An toàn Dữ liệu & Đám mây (Data Safety State Machine):**
  - **Firebase Cloud Sync Card:**
    - Trực quan hóa chế độ hoạt động hiện tại: Huy hiệu [LogAppStatusBadge] `Chế độ Cục bộ (Local Only)` giải thích rõ ràng toàn bộ dữ liệu đang nằm an toàn 100% trong SQLite/Room trên thiết bị.
    - Hiển thị chỉ số thay đổi cục bộ sẵn sàng đồng bộ: `$pendingSyncCount bản ghi sẵn sàng`.
    - Kiến trúc mở: Bắt nguồn trực tiếp từ [SyncMetadataRepository] và [SyncWorker]. Khi sau này kích hoạt Firebase credentials, State machine tự động chuyển sang `Syncing` -> `Synced` mà không cần sửa đổi giao diện.
  - **Google Drive Backup Card:**
    - Trực quan hóa thời gian sao lưu: `"Bản sao lưu gần nhất: Hôm nay 14:30"` (hoặc ngày/giờ cụ thể) đi kèm biểu tượng đám mây theo đúng mục tiêu SRS.
    - Trạng thái chưa kết nối: Huy hiệu `Chưa liên kết`, hiển thị thông điệp an tâm và nút "Liên kết tài khoản Google Drive" gắn trực tiếp với OAuth launcher.
    - Trạng thái đã kết nối: Hiển thị email tài khoản, nút "Sao lưu ngay" (có vòng xoay tiến trình khi đang backup), nút "Khôi phục" (mở danh sách file sao lưu trên Drive để tải về), cài đặt tự động sao lưu định kỳ (Hàng ngày/Hàng tuần, số bản lưu giữ), và nút ngắt kết nối an toàn.
- **Trải nghiệm Xuất / Nhập Tệp Ngoại tuyến Hoàn chỉnh (Local Export & Import Hub):**
  - **Thẻ Xuất Dữ liệu (Local Export):**
    - Xuất JSON: Sao lưu đầy đủ cấu trúc thực thể và quan hệ để khôi phục bất kỳ lúc nào. Tên file tự sinh theo timestamp `timelens_backup_yyyyMMdd_HHmmss.json`.
    - Xuất CSV: Định dạng bảng tính chuẩn để mở trên Excel / Google Sheets phục vụ phân tích cá nhân.
  - **Hộp thoại Xem trước khi Nhập (`ImportPreviewDialog`):**
    - Quét và kiểm tra định dạng tệp JSON, hiển thị thẻ thống kê tổng quan: Số hoạt động (Activities) và Số phiên ghi nhận (Sessions).
    - Bộ chọn chiến lược nhập dữ liệu rõ ràng:
      - **Gộp dữ liệu (Merge - Khuyên dùng):** Bổ sung dữ liệu mới, giữ nguyên 100% dữ liệu đang có trên máy.
      - **Ghi đè toàn bộ (Replace - Cảnh báo nguy hiểm):** Hộp thoại xác nhận lần 2 ([LogAppConfirmDialog]) cảnh báo xóa dữ liệu cũ trước khi thực hiện để đảm bảo an toàn tuyệt đối.
  - **Thanh phản hồi nhanh (Snackbar):** Thông báo trạng thái ngay sau khi xuất/nhập thành công kèm số lượng bản ghi cụ thể.
- **Tình trạng Sức khỏe Dữ liệu (Data Health & Self-Healing):**
  - Quét Room DB cục bộ, báo cáo tính toàn vẹn (Orphan sessions, Timestamps nhất quán), tích hợp nút "Tự động sửa lỗi dữ liệu (Self-Healing)".
- **Bộ Unit Tests hoàn chỉnh (`SettingsViewModelTest`):**
  - Đã cập nhật và bổ sung các ca kiểm thử cho `pendingSyncCount`, `lastBackupTime`, `lastBackupTimeFormatted`, `refreshPendingSyncs`, và Data Health.

### Phase 7: Loading / Error / Empty / Offline States

**Status:** `DONE`

**Objective:** Mọi màn hình phải có state model đầy đủ (Stateful UI: Loading, Error, Empty, Offline).
- **Design System Foundation cho Stateful UI (`core/designsystem/component/`):**
  - [LogAppSkeleton.kt]: Hiệu ứng chuyển màu mượt mà (`shimmerEffect()`) chạy tuần hoàn theo trục nghiêng chuẩn Material 3. Cung cấp các bộ khung placeholder:
    - `SkeletonActivityGrid`: Khung lưới hoạt động dạng ô vuông bo góc cho màn hình chính.
    - `SkeletonAnalytics`: Khung phân tích toàn diện gồm thẻ Hero tổng quan, biểu đồ đồng thời và danh sách hoạt động.
    - `SkeletonTimeline`: Khung thanh tóm tắt và các khối session của dòng thời gian.
    - `SkeletonActivityList`: Khung danh sách hàng hoạt động cho trang quản lý.
  - [LogAppEmptyState.kt]: Thành phần giao diện rỗng (Empty State) thống nhất: Icon/Illustration bo tròn trên nền màu container, Tiêu đề, Mô tả giải thích thân thiện, Nút hành động chính (Primary Action) và Nút phụ (Secondary Action).
  - [LogAppErrorState.kt]: Thành phần giao diện lỗi (Error State): Icon cảnh báo màu semantic error, Thông điệp giải thích ngắn gọn, Nút "Thử lại" (Retry) kích hoạt gọi lại hàm logic.
  - [LogAppOfflineBanner.kt]: Biểu ngữ thông báo ngoại tuyến có hiệu ứng trượt mượt mà (`AnimatedVisibility`), giải thích rõ ràng triết lý Offline-first: "Toàn bộ hoạt động và phiên được lưu trữ an toàn 100% trên thiết bị".
- **Hạ tầng Giám sát Kết nối Mạng (`core/network/`):**
  - Xây dựng [NetworkMonitor] và triển khai [ConnectivityNetworkMonitor] theo thời gian thực sử dụng `ConnectivityManager.NetworkCallback` và coroutine `callbackFlow`.
  - Tích hợp Hilt Singleton qua [NetworkModule].
- **Áp dụng Stateful UI cho toàn bộ các màn hình:**
  - **Màn hình chính (Home / Tracking):**
    - Biểu ngữ [LogAppOfflineBanner] hiển thị khi thiết bị ngoại tuyến.
    - Trạng thái tải: Hiển thị `SkeletonActivityGrid` tránh giật nhấp nháy hoặc hiển thị vội trạng thái rỗng khi DB đang đọc.
    - Trạng thái lỗi: Hiển thị [LogAppErrorState] với nút "Thử lại".
    - Trạng thái rỗng ngày: Gợi ý tạo động lực khi chưa track phiên nào hôm nay: *"Bạn chưa ghi nhận hoạt động nào hôm nay. Chạm một hoạt động bên dưới để bắt đầu!"*
    - Trạng thái chưa có hoạt động: Thẻ [LogAppEmptyState] kèm nút "+ Tạo hoạt động mới" và "Thêm hoạt động mẫu".
  - **Nhật ký (Timeline):**
    - Trạng thái tải: Hiển thị `SkeletonTimeline` khi chuyển ngày hoặc nạp dữ liệu.
    - Trạng thái lỗi: Hiển thị [LogAppErrorState] với nút "Thử lại".
    - Trạng thái rỗng: Hiển thị [LogAppEmptyState] với nút "+ Thêm phiên thủ công" và nút "Quay về Hôm nay".
    - Phản hồi thao tác: Báo lỗi trực quan qua Snackbar khi chia (Split) hoặc gộp (Merge) phiên không hợp lệ.
  - **Phân tích & Thống kê (Analytics):**
    - Trạng thái tính toán: Hiển thị `SkeletonAnalytics` thay cho vòng xoay đơn điệu khi xử lý lượng lớn dữ liệu ngày/tuần/tháng.
    - Trạng thái lỗi: Hiển thị [LogAppErrorState] với nút "Thử lại".
    - Trạng thái rỗng: Chuẩn hóa [AnalyticsEmptyStateCard] sử dụng [LogAppEmptyState] cho Ngày, Tuần, Tháng.
  - **Cài đặt & Sao lưu (Settings):**
    - Trạng thái lỗi sao lưu Google Drive: Hiển thị thông báo lỗi rõ ràng đi kèm nút **[Thử lại (Retry)]** kích hoạt sao lưu tức thì (tuân thủ SRS #73).
    - Hộp thoại bản sao lưu Google Drive: Hiển thị bộ khung shimmer khi đang nạp danh sách, và hiển thị [LogAppEmptyState] khi chưa có tệp sao lưu trên đám mây.
  - **Quản lý hoạt động (Manage Activities):**
    - Trạng thái tải: Hiển thị `SkeletonActivityList` trên cả tab "Đang theo dõi" và "Đã lưu trữ".
    - Trạng thái lỗi: Hiển thị [LogAppErrorState] với nút "Thử lại".
    - Trạng thái rỗng: Chuẩn hóa thẻ rỗng tìm kiếm, rỗng theo dõi và rỗng lưu trữ theo [LogAppEmptyState].
- **Bộ Unit Tests hoàn chỉnh (`StatefulUiViewModelTest`):**
  - Kiểm thử `isLoading`, `errorMessage`, `isOnline`, `retry`, `jumpToToday` cho tất cả các ViewModel.
  - Sửa và chuẩn hóa các bài kiểm thử `DriveBackupUseCaseTest` và `ManageActivitiesViewModelTest`, đạt 100% tỷ lệ pass toàn bộ dự án (97/97 tests passed).


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
