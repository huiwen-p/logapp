# 1. Tổng quan

## 1.1. Tên hệ thống

**Personal Time Tracking & Activity Analytics**

Tên app tạm thời: **TimeLens**

## 1.2. Mục đích

TimeLens là ứng dụng Android cá nhân dùng để ghi nhận:

* người dùng đang làm gì;
* hoạt động bắt đầu lúc nào;
* hoạt động kết thúc lúc nào;
* nhiều hoạt động có thể xảy ra đồng thời;
* các khoảng thời gian chưa được ghi nhận;
* lịch sử hoạt động theo ngày/tuần/tháng;
* các mối quan hệ và pattern giữa các hoạt động.

Ứng dụng không nhằm:

* chấm điểm năng suất;
* đánh giá người dùng tốt/xấu;
* ép người dùng xây dựng habit;
* tự động quyết định người dùng nên làm gì;
* thay thế người dùng trong việc lập kế hoạch cuộc sống.

Triết lý sản phẩm:

> **Record reality → Visualize reality → Understand reality.**

---

# 2. Phạm vi hệ thống

## 2.1. Phạm vi chính

Hệ thống bao gồm:

1. Activity management
2. Time tracking
3. Concurrent tracking
4. Session editing
5. Timeline visualization
6. Daily analytics
7. Weekly analytics
8. Monthly analytics
9. Overlap analysis
10. Concurrency analysis
11. Activity relationship analysis
12. Notes
13. JSON export
14. CSV export
15. JSON import
16. Local backup/restore
17. Firebase authentication
18. Firebase cloud synchronization
19. Firebase cloud backup
20. Google Drive file backup
21. Google Drive restore
22. Data conflict handling
23. Settings
24. Crash/error monitoring
25. Data integrity checking

---

# 3. Kiến trúc tổng thể

## 3.1. Kiến trúc

```text
┌───────────────────────────────────────────┐
│              Android Application          │
│                                           │
│ Kotlin + Jetpack Compose + Material 3    │
│                                           │
│ UI                                        │
│  ↓                                        │
│ ViewModel / StateFlow                     │
│  ↓                                        │
│ Use Cases                                 │
│  ↓                                        │
│ Repository                                │
│  ↓                                        │
│ ┌──────────────────┐                      │
│ │ Room / SQLite    │ ← SOURCE OF TRUTH    │
│ └────────┬─────────┘                      │
│          │                                │
│     ┌────┴───────────────┐                │
│     │                    │                │
│     ▼                    ▼                │
│ Firebase             Backup Engine        │
│ Firestore             │                   │
│     │                 ├── JSON            │
│     │                 ├── CSV             │
│     │                 └── Drive API       │
│     │                                     │
│     └── Cloud Sync / Backup               │
│                                           │
└───────────────────────────────────────────┘
```

## 3.2. Nguyên tắc Source of Truth

Room là nguồn dữ liệu chính của ứng dụng.

Mọi thao tác tracking phải được ghi vào Room trước.

Ví dụ:

```text
User tap "Học"
       ↓
StartActivityUseCase
       ↓
Room INSERT
       ↓
Room transaction success
       ↓
UI update
       ↓
Sync queue
       ↓
Firebase / Drive
```

Không được thiết kế:

```text
User tap
   ↓
Internet
   ↓
Firebase
   ↓
response
   ↓
Room
```

Tracking phải hoạt động đầy đủ khi offline.

---

# 4. Technology Stack

| Thành phần        | Công nghệ                                             |
| ----------------- | ----------------------------------------------------- |
| Language          | Kotlin                                                |
| UI                | Jetpack Compose                                       |
| Design            | Material 3                                            |
| Architecture      | UDF + ViewModel + Repository                          |
| Local DB          | Room / SQLite                                         |
| Preferences       | DataStore                                             |
| Cloud DB          | Firebase Firestore                                    |
| Authentication    | Firebase Authentication                               |
| Background tasks  | WorkManager                                           |
| Serialization     | kotlinx.serialization                                 |
| Cloud file backup | Google Drive API                                      |
| Charts            | Compose Canvas / custom components                    |
| Navigation        | Navigation Compose                                    |
| Crash monitoring  | Firebase Crashlytics                                  |
| Security          | Firebase App Check                                    |
| Testing           | JUnit + Android instrumented tests + Compose UI tests |
| Build             | Gradle Kotlin DSL                                     |
| CI                | GitHub Actions                                        |

Google Drive API cho phép ứng dụng tạo và upload file trực tiếp lên Drive; với backup file của chính ứng dụng, scope `drive.file` là hướng ưu tiên vì giới hạn quyền truy cập theo file thay vì yêu cầu quyền với toàn bộ Drive.

---

# 5. Người dùng hệ thống

## 5.1. User

Phiên bản hiện tại chỉ có một loại người dùng:

**Owner/User**

Người dùng có toàn quyền:

* tạo Activity;
* sửa Activity;
* xóa Activity;
* tracking;
* chỉnh sửa session;
* export;
* import;
* backup;
* restore;
* sync;
* quản lý tài khoản.

Không có:

* admin;
* moderator;
* social user;
* follower;
* public profile.

---

# 6. Core Domain

## 6.1. Activity

Activity đại diện cho một loại hoạt động mà người dùng muốn theo dõi.

Ví dụ:

```text
Học
Work
TikTok
Game
Ăn
Ngủ
Đi bộ
Nghe nhạc
Đọc sách
```

### Fields

```text
Activity
-------------------------
id
name
icon
color
categoryId
sortOrder
isArchived
createdAt
updatedAt
```

### Business rules

* `name` không được rỗng.
* Activity đã archive không xuất hiện trong danh sách quick tracking mặc định.
* Session cũ của Activity đã archive vẫn phải tồn tại.
* Không được xóa Activity nếu việc xóa làm mất session lịch sử.
* Khi user chọn Delete Activity, hệ thống phải cho phép:

  * archive Activity; hoặc
  * xóa Activity và xử lý session liên quan theo quy tắc explicit.

Khuyến nghị:

> **Archive thay vì hard delete.**

---

# 7. Activity Session

`ActivitySession` là entity trung tâm của hệ thống.

```text
ActivitySession
-------------------------
id
activityId
startedAt
endedAt
note
createdAt
updatedAt
deletedAt
syncStatus
version
```

## 7.1. Session đang chạy

Nếu:

```text
endedAt == null
```

thì session đang active.

Ví dụ:

```text
Học
08:00 → null
```

## 7.2. Session đã hoàn thành

```text
Học
08:00 → 10:00
```

## 7.3. Session không bị merge tự động

Ví dụ:

```text
Học
08:00 → 08:40

Học
09:00 → 10:00

Học
14:00 → 15:20
```

phải được giữ thành 3 session riêng biệt.

Điều này cho phép tính:

* số session;
* average duration;
* median duration;
* longest session;
* shortest session;
* interruption;
* overlap.

Thiết kế này phù hợp với domain đã xác định trước đó: một Activity có thể có nhiều session độc lập.

---

# 8. Timestamp

Mọi timestamp phải được lưu dưới dạng absolute timestamp.

Khuyến nghị:

```text
Instant
```

Không lưu dữ liệu cốt lõi chỉ bằng:

```text
08:00
```

Mà lưu:

```text
2026-09-30T08:00:00+07:00
```

hoặc canonical UTC tương ứng.

Mục đích:

* timezone;
* export;
* import;
* cloud sync;
* multi-device;
* daylight saving;
* thay đổi timezone;
* phân tích theo ngày.

Timezone của user phải được lưu trong metadata của export.

---

# 9. Tracking

## 9.1. Start Activity

Khi user tap Activity chưa active:

```text
START
```

Hệ thống:

1. lấy current timestamp;
2. tạo ActivitySession;
3. lưu vào Room;
4. cập nhật UI;
5. đưa thay đổi vào sync queue.

## 9.2. Stop Activity

Khi user tap Activity đang active:

```text
STOP
```

Hệ thống:

1. lấy current timestamp;
2. update `endedAt`;
3. lưu Room;
4. cập nhật UI;
5. đưa thay đổi vào sync queue.

## 9.3. Không giới hạn số active session

Ví dụ đồng thời:

```text
Học       08:00 → active
Nhạc      08:30 → active
Podcast   08:40 → active
```

Hệ thống phải cho phép.

Đây là yêu cầu bắt buộc vì overlap là một phần của domain, không phải edge case.

---

# 10. Timer

Database không được update mỗi giây.

Database chỉ lưu:

```text
startedAt
```

UI tính:

```text
currentTime - startedAt
```

Ví dụ:

```text
startedAt = 08:00:00

currentTime = 09:23:41

display = 01:23:41
```

Khi app bị kill/restart:

```text
Room
 ↓
startedAt
 ↓
currentTime - startedAt
```

timer tiếp tục chính xác.

---

# 11. Quick Tracking

Mục tiêu:

> Một thao tác tracking thông thường phải gần như chỉ cần một tap.

Home screen phải ưu tiên:

```text
[ Học ]
[ Work ]
[ TikTok ]
[ Game ]
[ Ăn ]
[ Đi bộ ]
```

Không được bắt user nhập:

```text
duration
start time
goal
reason
note
```

trước khi bắt đầu tracking.

Các thông tin phụ là optional.

---

# 12. Active Sessions

Home screen phải có khu vực:

```text
ĐANG THEO DÕI
```

Ví dụ:

```text
┌────────────────────────────┐
│ 📚 Học                     │
│ 01:23:42                   │
│ 08:00 → hiện tại           │
│                    [STOP]  │
└────────────────────────────┘

┌────────────────────────────┐
│ 🎵 Nghe nhạc               │
│ 00:42:10                   │
│ 08:41 → hiện tại           │
│                    [STOP]  │
└────────────────────────────┘
```

Active session phải được khôi phục sau:

* app restart;
* process death;
* device restart nếu hệ thống cho phép;
* temporary offline.

---

# 13. Undo

Sau Stop:

```text
Đã kết thúc

TikTok
08:40 → 09:00
20 phút

[Hoàn tác]
```

Undo phải khôi phục session về trạng thái trước thao tác.

Undo chỉ là convenience UI.

Dữ liệu chính vẫn phải có history/version nếu hệ thống audit được bật.

---

# 14. Manual Session Editing

User có thể chỉnh:

* start time;
* end time;
* Activity;
* note.

Ví dụ:

```text
Activity: Học

Start:
[14:00]

End:
[15:20]

Note:
[Redis chapter 2]
```

---

# 15. Timeline

Timeline là màn hình cốt lõi.

## 15.1. Yêu cầu

Timeline phải thể hiện:

* Activity;
* session;
* start;
* end;
* overlap;
* active session;
* gaps.

Ví dụ:

```text
07:00      09:00      11:00      13:00

Học       █████████████

TikTok           ███

Nhạc                 ███████

Ăn                           ███

Work                              ███████
```

## 15.2. Zoom

Hỗ trợ:

* zoom in;
* zoom out;
* horizontal scrolling.

## 15.3. Session interaction

Tap session:

```text
┌──────────────────────┐
│ 📚 Học               │
│                      │
│ 08:00 → 08:40        │
│ Duration: 40m        │
│                      │
│ Redis chapter 2      │
│                      │
│ [Edit] [Delete]      │
└──────────────────────┘
```

---

# 16. Session Operations

Timeline phải hỗ trợ:

### Add

Tạo session thủ công.

### Edit

Sửa:

```text
start
end
activity
note
```

### Delete

Xóa session.

### Split

Ví dụ:

```text
08:00 → 10:00
```

thành:

```text
08:00 → 09:00
09:20 → 10:00
```

### Merge

Hai session cùng Activity có thể được merge thủ công.

### Change Activity

```text
Học
↓
Work
```

không tạo session mới nếu user chỉ đang sửa dữ liệu lịch sử.

---

# 17. Overlap Engine

Hệ thống phải xác định giao nhau giữa các session.

Cho:

```text
A = [08:00, 10:00]
B = [08:40, 09:00]
```

Overlap:

```text
[08:40, 09:00]
```

Duration:

```text
20 minutes
```

Với:

```text
A = 08:00 → 10:00
B = 08:40 → 09:00
C = 08:50 → 09:30
```

Timeline engine phải tạo segments:

```text
08:00 → 08:40
A

08:40 → 08:50
A + B

08:50 → 09:00
A + B + C

09:00 → 09:30
A + C

09:30 → 10:00
A
```

---

# 18. Analytics Engine

Analytics phải được tách khỏi UI.

Input:

```text
List<ActivitySession>
```

Output:

```text
DailyAnalytics
```

bao gồm:

```text
trackedDuration
uniqueClockDuration
overlapDuration
untrackedDuration

activityBreakdown
sessionStatistics
overlapPairs
concurrencyDistribution
timelineSegments
```

Thiết kế này kế thừa trực tiếp analytics engine đã xác định trước đó.

---

# 19. Các loại thời gian

## 19.1. Tracked Duration

Tổng duration của tất cả session.

Ví dụ:

```text
Học 3h
Work 4h
Nhạc 2h
```

Tracked:

```text
9h
```

## 19.2. Unique Clock Duration

Khoảng thời gian thực tế đã trôi qua có ít nhất một activity đang được tracking.

## 19.3. Overlap Duration

Khoảng thời gian có từ hai activity trở lên.

## 19.4. Untracked Duration

Khoảng thời gian thuộc phạm vi phân tích nhưng không có activity được tracking.

---

# 20. Concurrency Analysis

Hệ thống phải tính:

```text
1 activity
2 activities
3 activities
4+ activities
```

Ví dụ:

```text
1 activity    6h20
2 activities  1h05
3 activities  0h25
4+            0h00
```

Không được gọi đây là:

```text
multitasking score
```

vì hệ thống chỉ mô tả dữ liệu.

---

# 21. Activity Statistics

Cho mỗi Activity:

```text
Total duration
Session count
Average duration
Median duration
Longest session
Shortest session
First occurrence
Last occurrence
```

Ví dụ:

```text
Học

Total:       48h20
Sessions:    42
Average:     51m
Median:      38m
Longest:     2h20
Shortest:    4m
```

---

# 22. Activity Relationship

Sau khi đủ dữ liệu, hệ thống có thể thống kê:

```text
Activity A × Activity B
```

Ví dụ:

```text
Học × Nhạc
1h20 overlap

Ăn × TikTok
15m overlap

Work × Nhạc
1h10 overlap
```

Ngoài overlap trực tiếp, hệ thống có thể thống kê:

* Activity B xuất hiện trong X phút sau Activity A;
* Activity B thường xuất hiện trước Activity A;
* frequency;
* time-of-day distribution.

Không được tự diễn giải causal relationship.

Ví dụ hệ thống không được nói:

> “TikTok khiến bạn mất tập trung.”

Chỉ được nói:

> “TikTok xuất hiện trong 7 session học trong khoảng dữ liệu được chọn.”

---

# 23. Daily View

Mỗi ngày hiển thị:

```text
Date

Tracked
Unique time
Overlap
Untracked
```

và:

```text
Activity breakdown
Session statistics
Timeline
Overlap
Concurrency
```

---

# 24. Daily Note

User có thể viết:

```text
Hôm nay có gì đáng nhớ?
```

Ví dụ:

```text
Hôm nay đi làm về khá mệt.
```

Daily Note là dữ liệu optional.

Không ép user review.

---

# 25. Weekly View

Hiển thị:

```text
Mon Tue Wed Thu Fri Sat Sun
```

theo Activity.

Ví dụ:

```text
        T2  T3  T4  T5  T6  T7  CN

Học     3h  2h  4h  3h  1h  2h  0h
Work    5h  6h  5h  4h  6h  0h  0h
Game    1h  2h  0h  3h  1h  4h  2h
```

Có thể xem:

* total;
* daily average;
* session count;
* distribution by hour.

---

# 26. Monthly View

Cho phép:

```text
01/09 → 30/09
```

và hiển thị:

* total Activity duration;
* session count;
* median session;
* overlap;
* daily distribution;
* weekday distribution;
* hour-of-day distribution.

---

# 27. Search

User có thể tìm:

```text
Redis
```

để tìm session có:

* Activity;
* note;
* date.

Ví dụ:

```text
Redis

14/09
08:00 → 08:40
Note: Redis chapter 2

18/09
14:00 → 15:20
Note: Redis Streams
```

---

# 28. Export

Export phải hỗ trợ:

### JSON

Dùng làm:

* backup;
* restore;
* data portability;
* AI analysis.

### CSV

Dùng làm:

* Excel;
* Google Sheets;
* Python;
* data analysis.

---

# 29. JSON Schema

JSON phải có:

```json
{
  "schema_version": 1,
  "exported_at": "...",
  "timezone": "Asia/Ho_Chi_Minh",

  "activities": [],
  "sessions": [],
  "daily_notes": [],
  "settings": {}
}
```

`schema_version` bắt buộc.

Mục tiêu:

```text
JSON v1
   ↓
future
   ↓
JSON v2
```

App phải có migration strategy.

---

# 30. Export theo phạm vi

Cho phép:

```text
Today
Yesterday
This week
This month
Custom range
All data
```

Ví dụ:

```text
01/09/2026 → 30/09/2026
```

---

# 31. Export Center

Màn hình:

```text
EXPORT CENTER

Range

○ Today
○ This week
○ This month
○ Custom
○ All data

Format

○ JSON
○ CSV

[EXPORT]
[SHARE]
[BACKUP TO DRIVE]
```

---

# 32. Local Backup

Local backup tạo một file:

```text
timelens_backup_2026-09-30.json
```

Có thể lưu/share thông qua Android system file picker/share sheet.

Backup file phải chứa:

* schema version;
* activities;
* sessions;
* notes;
* settings;
* metadata.

Không export dữ liệu database raw.

---

# 33. Google Drive Backup

Đây là yêu cầu mới được bổ sung.

Google Drive backup là **file backup**, độc lập với Firebase.

Kiến trúc:

```text
Room
 ↓
BackupSerializer
 ↓
JSON
 ↓
DriveBackupManager
 ↓
Google Drive
```

Google Drive API hỗ trợ tạo file và upload nội dung; với file backup nhỏ, multipart upload phù hợp, còn resumable upload phù hợp hơn nếu file lớn hoặc mạng di động không ổn định.

---

# 34. Drive Backup Folder

App nên tạo một folder riêng:

```text
My Drive/
└── TimeLens/
    ├── backups/
    │   ├── timelens_backup_latest.json
    │   ├── timelens_backup_2026-09-29.json
    │   └── timelens_backup_2026-09-30.json
    │
    └── exports/
        ├── timelens_export_2026-09.json
        └── timelens_export_2026-09.csv
```

Không được yêu cầu quyền đọc toàn bộ Drive nếu không cần.

Ưu tiên scope:

```text
drive.file
```

Google hiện khuyến nghị scope hẹp như `drive.file` khi phù hợp.

---

# 35. Drive Authentication

User chọn:

```text
Connect Google Drive
```

App:

1. yêu cầu Google authorization;
2. xin quyền Drive cần thiết;
3. lưu token/credential theo cơ chế bảo mật Android;
4. tạo/nhận diện folder TimeLens;
5. lưu Drive folder ID;
6. sẵn sàng backup.

Firebase Google Authentication và Google Drive authorization phải được coi là **hai quyền khác nhau**.

Không được giả định:

```text
Google Sign-In
=
tự động có quyền Drive
```

---

# 36. Manual Drive Backup

User bấm:

```text
Backup now
```

Flow:

```text
Room
 ↓
Validate
 ↓
Serialize
 ↓
JSON
 ↓
Upload Drive
 ↓
Verify upload
 ↓
Update BackupMetadata
```

UI:

```text
Backup successful

30/09/2026 11:20

Records: 2,381
File: 1.8 MB

[Open in Drive]
```

---

# 37. Automatic Drive Backup

Settings:

```text
Automatic Drive Backup

[ ON ]

Frequency:
○ Daily
○ Weekly
○ Manual only
```

Khuyến nghị mặc định:

```text
Weekly
```

hoặc:

```text
Daily
```

nếu dữ liệu thay đổi nhiều.

Background backup sử dụng WorkManager.

---

# 38. Backup Rotation

Không nên tạo vô hạn file:

```text
backup_1
backup_2
backup_3
...
backup_500
```

Settings:

```text
Keep:
○ Latest only
○ Latest 7
○ Latest 30
○ All
```

Mặc định:

```text
Latest 7
```

---

# 39. Drive Restore

Flow:

```text
Settings
 ↓
Backup & Restore
 ↓
Google Drive
 ↓
Select backup
 ↓
Validate
 ↓
Preview
 ↓
Confirm
 ↓
Restore
```

Preview:

```text
Backup date:
30/09/2026

Activities:
18

Sessions:
2,381

Notes:
42

Time range:
01/01/2026 → 30/09/2026
```

User phải xác nhận trước khi overwrite/merge.

---

# 40. Restore Strategy

Hệ thống hỗ trợ hai mode:

## Replace

```text
Current local data
        ↓
REPLACED
        ↓
Backup data
```

## Merge

```text
Current local data
        +
Backup data
        ↓
Deduplicate
        ↓
Merged dataset
```

Default:

> **Preview trước, không tự động overwrite.**

---

# 41. Backup Integrity

Mỗi backup phải có metadata:

```text
schemaVersion
createdAt
deviceId
recordCount
checksum
```

Ví dụ:

```json
{
  "schema_version": 1,
  "record_count": 2381,
  "checksum": "...",
  "created_at": "..."
}
```

Mục tiêu:

* phát hiện file hỏng;
* phát hiện backup không đầy đủ;
* kiểm tra restore.

---

# 42. Firebase

Firebase gồm:

```text
Firebase Authentication
Firebase Firestore
Firebase Crashlytics
Firebase App Check
```

Không bắt buộc:

```text
Firebase Analytics
FCM
```

trừ khi sau này có nhu cầu.

---

# 43. Firebase Authentication

Google Sign-In.

User có thể:

```text
Sign in
Sign out
```

Account dùng để xác định:

```text
userId
```

Không dùng email làm primary key.

---

# 44. Firestore Data Model

Đề xuất:

```text
users/{userId}

users/{userId}/activities/{activityId}

users/{userId}/sessions/{sessionId}

users/{userId}/daily_notes/{noteId}

users/{userId}/settings/{settingId}
```

Không dùng một document chứa toàn bộ sessions.

---

# 45. Firestore Session

Ví dụ:

```json
{
  "id": "...",
  "activityId": "study",
  "startedAt": "...",
  "endedAt": "...",
  "note": "...",
  "createdAt": "...",
  "updatedAt": "...",
  "deletedAt": null,
  "version": 4
}
```

---

# 46. Sync Model

Room:

```text
LOCAL
```

Firestore:

```text
REMOTE
```

Mỗi local mutation tạo:

```text
SyncMetadata
```

Ví dụ:

```text
entityId
entityType
operation
localVersion
syncStatus
lastAttemptAt
lastSyncedAt
error
```

Status:

```text
PENDING
SYNCING
SYNCED
FAILED
CONFLICT
```

---

# 47. Sync Flow

```text
User action
    ↓
Room transaction
    ↓
SYNC_PENDING
    ↓
WorkManager
    ↓
Internet available?
   / \
 NO   YES
 |     |
wait   Firestore
       ↓
    success
       ↓
   SYNCED
```

Không được để Firebase failure làm mất local data.

---

# 48. Offline Firebase

Firestore cũng hỗ trợ offline access và đồng bộ local changes khi mạng trở lại.

Tuy nhiên app vẫn phải giữ abstraction:

```text
Room
 ↓
Sync layer
 ↓
Firestore
```

thay vì để UI phụ thuộc trực tiếp vào Firestore.

---

# 49. Conflict Resolution

Conflict xảy ra khi:

```text
Phone A
   ↓
edit session

Phone B
   ↓
edit same session
```

Có thể xảy ra khi sau này app chạy nhiều thiết bị.

Mỗi entity có:

```text
updatedAt
version
```

Chiến lược mặc định:

```text
Last-write-wins
```

nhưng phải ghi nhận conflict nếu phát hiện version mismatch.

Không silently overwrite trong trường hợp conflict nghiêm trọng.

---

# 50. Soft Delete

Session không nên hard delete ngay trong cloud sync.

Thay vì:

```text
DELETE
```

dùng:

```text
deletedAt
```

Ví dụ:

```text
deletedAt = 2026-09-30T10:30:00Z
```

Sync:

```text
Room
 ↓
Firestore
```

Sau khi tất cả thiết bị đã đồng bộ và quá retention period mới purge.

---

# 51. Firebase Backup vs Google Drive Backup

Hai hệ thống có vai trò khác nhau.

|                 | Firebase                  | Google Drive          |
| --------------- | ------------------------- | --------------------- |
| Mục đích        | Sync/cloud data           | File backup           |
| Format          | Firestore documents       | JSON/CSV              |
| Multi-device    | Có                        | Có thể restore        |
| Human-readable  | Không tối ưu              | Có                    |
| Offline app     | Có                        | Không phải DB runtime |
| Restore toàn bộ | Có                        | Có                    |
| Export dataset  | Không phải mục tiêu chính | Có                    |
| Portable        | Thấp hơn                  | Cao                   |
| Dependency      | Firebase                  | Google Drive          |

Vì vậy:

> **Không nên chọn một trong hai. Nếu bạn đã muốn cả Firebase và Drive, hãy cho chúng hai trách nhiệm khác nhau.**

---

# 52. Backup Architecture

```text
                      ROOM
                       │
            ┌──────────┴──────────┐
            │                     │
            ▼                     ▼
      Firebase Sync          Export Engine
            │                     │
            ▼                     ▼
       Firestore                 JSON
                                  │
                         ┌────────┴────────┐
                         │                 │
                         ▼                 ▼
                    Local File       Google Drive
```

Firebase bảo vệ dữ liệu cloud.

Drive giữ **file backup độc lập**.

Điều này tạo ra redundancy:

```text
Phone
+
Firestore
+
Google Drive
```

---

# 53. Import JSON

User có thể chọn:

```text
Import JSON
```

Hệ thống:

1. đọc file;
2. kiểm tra schema version;
3. validate JSON;
4. validate entities;
5. kiểm tra timestamp;
6. kiểm tra references;
7. preview;
8. cho user chọn Merge/Replace;
9. transaction;
10. cập nhật analytics.

Nếu validation fail:

```text
Import failed

Reason:
Session abc references unknown Activity xyz.
```

Không được import một phần dữ liệu một cách âm thầm.

---

# 54. CSV

CSV dành cho external analysis.

Columns:

```text
session_id
activity_id
activity_name
start
end
duration_seconds
note
created_at
updated_at
```

CSV export không nhất thiết phải là format restore.

---

# 55. Data Validation

Hệ thống phải kiểm tra:

### Activity

```text
name != empty
```

### Session

```text
activityId exists
startedAt != null
endedAt >= startedAt
```

Nếu active:

```text
endedAt == null
```

### Timestamp

Không chấp nhận malformed timestamp.

---

# 56. Day Boundary

Default:

```text
00:00
```

User có thể cấu hình:

```text
Personal day starts at:
04:00
```

Khi đó:

```text
03:30 01/10
```

có thể thuộc:

```text
Personal day: 30/09
```

Setting này phải được dùng nhất quán cho Daily Analytics.

---

# 57. Settings

## General

```text
Theme
Language
Start of day
Time format
```

## Tracking

```text
Default activities
Quick tracking order
Confirmation behavior
```

## Backup

```text
Firebase sync
Google Drive
Automatic backup
Backup frequency
Backup retention
```

## Data

```text
Export
Import
Restore
Delete all data
```

## Account

```text
Google account
Firebase status
Drive status
Sign out
```

---

# 58. Privacy

Mặc định:

* dữ liệu tracking nằm local;
* không public;
* không chia sẻ với user khác;
* không gửi dữ liệu tracking tới bên thứ ba ngoài các cloud services mà user bật.

Firebase chỉ được sử dụng cho chức năng cloud đã công bố.

Google Drive chỉ được truy cập trong phạm vi quyền mà user cấp.

Không thu thập dữ liệu behavior cho advertising.

---

# 59. Security

## Local

* Room database private app storage.
* Không lưu credential plaintext.
* OAuth tokens phải được quản lý bằng Android credential/security mechanisms.

## Firebase

* Firestore Security Rules phải giới hạn:

```text
request.auth.uid == userId
```

## Drive

Ưu tiên:

```text
drive.file
```

thay vì:

```text
drive
```

vì quyền `drive` cho phép truy cập rộng hơn đáng kể. Google khuyến nghị dùng scope hẹp nhất phù hợp với use case.

---

# 60. Crash Monitoring

Firebase Crashlytics theo dõi:

* crash;
* fatal exception;
* non-fatal exception.

Không gửi raw activity notes vào crash logs.

Không log dữ liệu nhạy cảm.

---

# 61. WorkManager

WorkManager dùng cho:

```text
Firebase sync
Drive backup
cleanup
retry failed upload
periodic reconciliation
```

Không dùng WorkManager để:

```text
count timer every second
```

Timer chỉ dựa trên:

```text
startedAt
currentTime
```

---

# 62. Notifications

Không bắt buộc notification để tracking.

Nếu active session:

```text
📚 Học
01:23:42
[Dừng]
```

có thể xuất hiện notification persistent nếu user bật.

Notification phải cho phép:

```text
STOP
```

mà không cần mở app.

---

# 63. Widget

Future/optional nhưng architecture phải cho phép:

```text
Current activity
Duration
Start/Stop
Quick activity
```

---

# 64. Quick Settings Tile

Optional.

Ví dụ:

```text
[ 📚 Học ]
```

tap:

```text
START
```

hoặc:

```text
STOP
```

---

# 65. Home Screen Experience

Màn hình mặc định:

```text
TODAY

Active Sessions

Quick Activities

Today Summary

Timeline preview

Navigation:
Track | Timeline | Analytics | Settings
```

Priority:

1. Start/stop nhanh.
2. Xem active sessions.
3. Xem ngày hiện tại.
4. Truy cập timeline.

Không đặt analytics nặng lên Home.

---

# 66. Analytics Screen

Navigation:

```text
Day
Week
Month
Custom
```

Sections:

```text
Overview
Activities
Sessions
Overlap
Concurrency
Patterns
```

---

# 67. Không có Productivity Score

Không có:

```text
Productivity: 87/100
```

Không có:

```text
Good day
Bad day
Failed day
```

Analytics chỉ mô tả dữ liệu.

---

# 68. AI Integration

AI không phải core database.

Nếu có AI integration trong tương lai:

```text
Room
 ↓
Analytics / Export
 ↓
JSON
 ↓
AI
```

AI không được quyền tự thay đổi raw tracking data.

AI chỉ phân tích.

Prompt mặc định nên yêu cầu:

```text
Describe observations.
Separate observations from interpretations.
Do not infer motivation.
Do not moralize.
Do not assume activities are good or bad.
State when evidence is insufficient.
```

---

# 69. Performance Requirements

## Tracking

Start/Stop phải phản hồi UI gần như tức thời.

Mục tiêu:

```text
tap → local persistence → UI update
```

trong phạm vi vài chục ms đến mức người dùng không cảm nhận được độ trễ mạng.

## Timeline

Timeline phải vẫn usable với:

```text
10,000+
```

sessions.

## Analytics

Daily analytics phải chạy nhanh với dataset lớn.

Không query toàn bộ database nếu chỉ xem một ngày.

---

# 70. Database Indexes

Index đề xuất:

```text
ActivitySession(activityId)

ActivitySession(startedAt)

ActivitySession(endedAt)

ActivitySession(startedAt, endedAt)

ActivitySession(updatedAt)

ActivitySession(syncStatus)
```

Có thể bổ sung index dựa trên profiling thực tế.

---

# 71. Transaction Requirements

Các thao tác sau phải atomic:

```text
Start session
Stop session
Edit session
Delete session
Import
Restore
Merge
Split
```

Ví dụ Stop:

```text
UPDATE session
+
UPDATE sync metadata
```

phải cùng transaction.

---

# 72. Error Handling

Nếu Room write fail:

```text
Không được báo tracking thành công.
```

Nếu Firebase upload fail:

```text
Local data vẫn giữ nguyên.
Sync status = FAILED/PENDING.
Retry sau.
```

Nếu Drive upload fail:

```text
Local backup file vẫn có thể được giữ.
Cloud backup status = FAILED.
```

Không được xóa local backup sau khi upload thất bại.

---

# 73. Backup Status

UI:

```text
Cloud Sync
● Synced

Google Drive
● Last backup:
30/09/2026 10:20

Local
● Healthy
```

Nếu lỗi:

```text
Google Drive
⚠ Backup failed

Reason:
Network unavailable

[Retry]
```

---

# 74. Data Health

Settings có:

```text
Data Health
```

Kiểm tra:

* orphan ActivitySession;
* invalid timestamps;
* duplicate IDs;
* malformed records;
* failed sync;
* incomplete backup.

Ví dụ:

```text
Data Health
────────────

✓ 2,381 sessions valid
✓ 18 activities valid
✓ No orphan records
✓ Last backup verified

Status: Healthy
```

---

# 75. Testing

## Unit tests

Bắt buộc test:

### Time

```text
duration
timezone
day boundary
```

### Overlap

```text
no overlap
partial overlap
complete overlap
nested overlap
3-way overlap
4+ overlap
```

### Analytics

```text
tracked duration
unique duration
overlap duration
untracked duration
```

### Import

```text
valid JSON
invalid JSON
old schema
unknown schema
missing activity
invalid timestamp
```

---

# 76. Example Overlap Tests

```text
A 08:00–10:00
B 11:00–12:00
→ overlap = 0
```

```text
A 08:00–10:00
B 09:00–11:00
→ overlap = 1h
```

```text
A 08:00–10:00
B 08:30–09:00
→ overlap = 30m
```

```text
A 08:00–10:00
B 08:30–09:00
C 08:45–09:30
```

phải tạo đúng concurrency segments.

---

# 77. Acceptance Criteria — Tracking

### AC-TRACK-001

Given:

```text
Activity = Học
```

When user taps Học.

Then:

```text
New ActivitySession created.
startedAt populated.
endedAt = null.
UI shows active timer.
```

### AC-TRACK-002

When user taps Học again.

Then:

```text
endedAt populated.
Session duration calculated.
UI removes active timer.
```

### AC-TRACK-003

If device is offline:

```text
Start/Stop still works.
```

### AC-TRACK-004

If app restarts:

```text
Active session is restored.
```

---

# 78. Acceptance Criteria — Overlap

Given:

```text
Học 08:00–10:00
TikTok 08:40–09:00
```

Then:

```text
Overlap = 20 minutes.
```

The system must not merge the sessions.

---

# 79. Acceptance Criteria — Export

Given:

```text
01/09 → 30/09
```

When user exports JSON.

Then file contains:

```text
schema_version
timezone
activities
sessions
daily_notes
settings
```

and can later be imported.

---

# 80. Acceptance Criteria — Google Drive

Given:

```text
Google Drive connected.
```

When user presses:

```text
Backup now
```

Then:

```text
JSON backup is generated.
File uploaded to TimeLens backup folder.
Drive file ID is stored.
Backup timestamp is updated.
UI reports success.
```

If network fails:

```text
Local data unchanged.
Backup marked failed/pending.
Retry available.
```

Google Drive API hỗ trợ cả tạo file và cập nhật file; với file backup lớn hoặc mạng không ổn định, resumable upload là cơ chế phù hợp.

---

# 81. Acceptance Criteria — Firebase

Given:

```text
User signed in.
```

When local session changes.

Then:

```text
Room updated immediately.
Sync operation queued.
Firestore eventually receives change.
```

If Firebase unavailable:

```text
Room continues working.
Sync retries later.
```

Firestore itself cũng có khả năng offline và tự đồng bộ local changes khi kết nối trở lại, nhưng hệ thống TimeLens vẫn coi Room là lớp dữ liệu vận hành chính.

---

# 82. Acceptance Criteria — Restore

Given:

```text
Valid backup.json
```

When user selects:

```text
Restore
```

Then system must:

1. validate;
2. show preview;
3. ask confirmation;
4. execute transaction;
5. update analytics;
6. update sync state.

Nếu file invalid:

```text
No database mutation.
```

---

# 83. Non-Functional Requirements

## Reliability

Không được mất tracking data chỉ vì:

* mất mạng;
* Firebase downtime;
* Drive unavailable;
* app restart.

## Portability

User có thể lấy raw data ra khỏi hệ thống bằng:

```text
JSON
CSV
```

## Maintainability

Business logic không được nằm trực tiếp trong Compose UI.

## Privacy

Không public tracking data.

## Extensibility

Có thể thêm:

* widget;
* Quick Settings;
* AI;
* multi-device;
* additional analytics;

mà không thay đổi core data model.

---

# 84. Package Structure

```text
app/
│
├── core/
│   ├── database/
│   ├── firebase/
│   ├── drive/
│   ├── serialization/
│   ├── time/
│   ├── security/
│   └── common/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── database/
│   │
│   ├── remote/
│   │   ├── firestore/
│   │   └── drive/
│   │
│   └── repository/
│
└── feature/
    ├── tracking/
    ├── timeline/
    ├── analytics/
    ├── activities/
    ├── export/
    ├── backup/
    ├── settings/
    └── account/
```

---

# 85. Core Use Cases

```text
StartActivity
StopActivity
GetActiveSessions

CreateActivity
UpdateActivity
ArchiveActivity

CreateManualSession
EditSession
DeleteSession
SplitSession
MergeSessions

GetDailyTimeline
GetWeeklyTimeline
GetMonthlyTimeline

CalculateDailyAnalytics
CalculateOverlap
CalculateConcurrency
CalculateActivityStatistics

ExportJson
ExportCsv
ImportJson

CreateLocalBackup
RestoreLocalBackup

ConnectGoogleDrive
BackupToDrive
RestoreFromDrive

SignIn
SignOut

SyncPendingChanges
ResolveConflict
```

---

# 86. Repository Interfaces

Ví dụ:

```kotlin
interface ActivityRepository {
    fun observeActivities(): Flow<List<Activity>>

    suspend fun create(activity: Activity)

    suspend fun update(activity: Activity)

    suspend fun archive(activityId: String)
}
```

```kotlin
interface SessionRepository {
    fun observeActiveSessions(): Flow<List<ActivitySession>>

    fun observeSessions(
        from: Instant,
        to: Instant
    ): Flow<List<ActivitySession>>

    suspend fun start(activityId: String): ActivitySession

    suspend fun stop(sessionId: String)

    suspend fun update(session: ActivitySession)

    suspend fun delete(sessionId: String)
}
```

---

# 87. Backup Repository

```kotlin
interface BackupRepository {

    suspend fun createJsonBackup(): File

    suspend fun exportCsv(
        from: Instant,
        to: Instant
    ): File

    suspend fun uploadToDrive(
        file: File
    ): BackupResult

    suspend fun restore(
        file: File,
        mode: RestoreMode
    ): RestoreResult
}
```

---

# 88. Firebase Repository

```kotlin
interface CloudSyncRepository {

    suspend fun uploadPendingChanges()

    suspend fun pullRemoteChanges()

    suspend fun reconcile()

    fun observeSyncStatus(): Flow<SyncStatus>
}
```

UI không biết Firestore tồn tại.

---

# 89. Navigation

```text
Home
│
├── Tracking
│
├── Timeline
│   ├── Day
│   ├── Week
│   └── Month
│
├── Analytics
│   ├── Overview
│   ├── Activities
│   ├── Overlap
│   ├── Concurrency
│   └── Patterns
│
└── Settings
    ├── Activities
    ├── Backup
    ├── Google Drive
    ├── Firebase
    ├── Export / Import
    ├── Account
    └── Data Health
```

---

# 90. Backup Strategy tổng thể

TimeLens phải có **3 lớp bảo vệ dữ liệu**:

```text
              ┌──────────────┐
              │     Room     │
              │   PRIMARY    │
              └──────┬───────┘
                     │
          ┌──────────┴───────────┐
          │                      │
          ▼                      ▼
     Firestore              JSON Backup
      CLOUD                    │
                              ▼
                         Google Drive
```

### Lớp 1 — Room

Dùng hàng ngày.

### Lớp 2 — Firestore

Cloud sync/remote copy.

### Lớp 3 — Google Drive

Portable file backup độc lập.

Đây là điểm tôi muốn giữ trong SRS vì **Firestore và Drive không nên được coi là cùng một chức năng**.

---

# 91. Backup Policy đề xuất

Mặc định:

```text
Local:
continuous

Firebase:
automatic sync when online

Google Drive:
automatic daily backup

Retention:
7 latest backups
```

User có thể đổi.

---

# 92. Disaster Recovery

Các trường hợp:

### Mất mạng

```text
Room → hoạt động bình thường
```

### Firebase lỗi

```text
Room → hoạt động bình thường
Drive → hoạt động bình thường
```

### Drive lỗi

```text
Room → hoạt động bình thường
Firebase → hoạt động bình thường
```

### Mất điện thoại

```text
Restore Firestore
hoặc
Restore Google Drive JSON
```

### Gỡ app

```text
Restore từ Firestore/Drive
```

---

# 93. Product Definition

TimeLens không phải:

```text
Habit tracker
To-do app
Calendar
Productivity score
Pomodoro app
Life coach
```

TimeLens là:

> **Personal activity recorder + time visualization + behavioral dataset.**

Core loop:

```text
DO
 ↓
RECORD
 ↓
VISUALIZE
 ↓
ANALYZE
 ↓
DECIDE FOR YOURSELF
```

---

# 94. Definition of Done

Phiên bản production đầu tiên được coi là hoàn thành khi:

### Tracking

* [ ] Start
* [ ] Stop
* [ ] Multiple active sessions
* [ ] Offline tracking
* [ ] App restart recovery
* [ ] Manual editing

### Timeline

* [ ] Day timeline
* [ ] Zoom
* [ ] Scroll
* [ ] Session detail
* [ ] Edit
* [ ] Delete
* [ ] Split
* [ ] Merge

### Analytics

* [ ] Activity duration
* [ ] Session statistics
* [ ] Overlap
* [ ] Concurrency
* [ ] Untracked time
* [ ] Weekly
* [ ] Monthly

### Data

* [ ] Room
* [ ] JSON export
* [ ] CSV export
* [ ] JSON import
* [ ] Validation
* [ ] Schema versioning

### Firebase

* [ ] Authentication
* [ ] Firestore
* [ ] Sync queue
* [ ] Offline handling
* [ ] Conflict handling
* [ ] Security rules
* [ ] Crashlytics

### Google Drive

* [ ] OAuth
* [ ] Connect Drive
* [ ] Create backup folder
* [ ] Manual backup
* [ ] Automatic backup
* [ ] Backup history
* [ ] Restore
* [ ] Backup validation

### Reliability

* [ ] Unit tests
* [ ] Integration tests
* [ ] UI tests
* [ ] Data health check
* [ ] Crash monitoring

### UX

* [ ] One-tap tracking
* [ ] No unnecessary confirmation
* [ ] Active sessions clearly visible
* [ ] Timeline understandable within seconds
* [ ] No productivity score
* [ ] No forced review
* [ ] No moral judgment

---

# 95. Final Product Experience

Một ngày sử dụng lý tưởng:

```text
Bạn sống
   ↓
1 tap
   ↓
App ghi timestamp
   ↓
Bạn tiếp tục sống
   ↓
1 tap
   ↓
App ghi timestamp
```

Cuối ngày:

```text
Timeline
   ↓
Bạn nhìn thấy ngày của mình
```

Sau 7 ngày:

```text
Weekly patterns
```

Sau 30 ngày:

```text
Behavioral dataset
```

Sau 90 ngày:

```text
Long-term personal time history
```

Và bất cứ lúc nào:

```text
Room
 ↓
JSON
 ↓
Google Drive
```

hoặc:

```text
Room
 ↓
Firestore
```

Dữ liệu không bị khóa trong ứng dụng.

---

# 96. Product Principle

> **TimeLens does not tell you how to live.**
>
> **It records what actually happened.**

Ứng dụng phải ưu tiên:

```text
Accuracy
Reliability
Low friction
Data ownership
Observability
Portability
Privacy
```

hơn:

```text
Gamification
Streak
Productivity score
Notifications
Artificial motivation
```
## X.X. Low-Friction Interaction & Minimum User Interaction

### X.X.1. Mục tiêu

Ứng dụng phải được thiết kế theo nguyên tắc **Minimum User Interaction**: giảm số lượng thao tác mà người dùng phải thực hiện để ghi nhận và xem lại hoạt động hằng ngày xuống mức tối thiểu.

Mục tiêu của hệ thống không phải yêu cầu người dùng liên tục quản lý dữ liệu, nhập thông tin hoặc điều hướng qua nhiều màn hình. Người dùng chỉ cần thực hiện hành động cần thiết để ghi nhận thực tế; các thao tác xử lý dữ liệu, tính toán thời lượng, đồng bộ và sao lưu phải được hệ thống tự động thực hiện.

**Nguyên tắc cốt lõi:**

> **Record first, configure later.**

Việc ghi nhận thời gian phải nhanh hơn và đơn giản hơn việc ghi chú thủ công.

---

### X.X.2. Nguyên tắc thiết kế

#### FR-LF-01 — Một thao tác cho thao tác ghi nhận cơ bản

Các thao tác thường xuyên phải yêu cầu tối đa **01 user action**.

| Thao tác                                 |                    Số thao tác mục tiêu |
| ---------------------------------------- | --------------------------------------: |
| Bắt đầu Activity đã tồn tại              |                                   1 tap |
| Dừng Activity đang chạy                  |                                   1 tap |
| Xem Activity đang chạy                   | Không yêu cầu mở màn hình quản lý riêng |
| Xem thời gian hôm nay                    |         Mở ứng dụng hoặc sử dụng Widget |
| Ghi nhận Activity từ Widget              |                                   1 tap |
| Ghi nhận Activity từ Notification Action |                                   1 tap |
| Đồng bộ Firebase                         |                                 Tự động |
| Backup Google Drive                      |                   Tự động theo cấu hình |
| Tính toán Analytics                      |                                 Tự động |

---

### X.X.3. Không bắt buộc nhập thông tin khi Tracking

#### FR-LF-02 — Không yêu cầu nhập dữ liệu khi Start/Stop

Khi người dùng bắt đầu hoặc kết thúc một Activity thông thường, hệ thống **MUST NOT** yêu cầu:

* nhập thời gian;
* nhập thời lượng;
* nhập ghi chú;
* chọn category;
* xác nhận bằng popup;
* nhấn nút Save;
* mở màn hình chỉnh sửa.

Ví dụ:

```text
Tap "Coding"
        ↓
Coding bắt đầu ngay
```

và:

```text
Tap "Coding"
        ↓
Coding kết thúc ngay
```

Các thông tin bổ sung như `note`, category hoặc chỉnh sửa timestamp chỉ được thực hiện **sau đó**, khi người dùng chủ động muốn bổ sung hoặc sửa dữ liệu.

---

### X.X.4. Không làm gián đoạn việc ghi nhận

#### FR-LF-03 — Tracking phải có độ ma sát thấp

Ứng dụng MUST ưu tiên khả năng ghi nhận nhanh hơn việc thu thập metadata.

Khi người dùng đang thực hiện một hoạt động, hệ thống không được yêu cầu người dùng hoàn thành một form trước khi session được ghi nhận.

Nếu có lỗi hoặc thiếu thông tin bổ sung, session vẫn phải được lưu với dữ liệu tối thiểu:

```text
activityId
startedAt
endedAt
```

Các metadata khác có thể được bổ sung hoặc chỉnh sửa sau.

---

### X.X.5. Hỗ trợ nhiều Activity đồng thời

#### FR-LF-04 — Không tự động kết thúc Activity khác

Do hệ thống hỗ trợ **overlapping sessions**, việc bắt đầu Activity mới **MUST NOT** mặc định kết thúc Activity đang chạy khác.

Ví dụ:

```text
08:00  Start Study
       ↓
08:40  Start TikTok
       ↓
09:00  Stop TikTok
       ↓
10:00  Stop Study
```

Kết quả:

```text
Study   08:00 ───────────────── 10:00
TikTok          08:40 ── 09:00
```

Hai session được lưu độc lập.

Do đó:

> **1 tap = 1 tracking event**

Người dùng không phải quản lý thủ công trạng thái của các Activity khác khi bắt đầu Activity mới.

---

### X.X.6. Giảm số lần mở ứng dụng

#### FR-LF-05 — Không bắt buộc mở App để Tracking

Các thao tác Tracking thường xuyên phải có thể thực hiện mà không cần mở màn hình chính của ứng dụng.

Hệ thống SHOULD hỗ trợ:

1. Android App Widget;
2. Notification Actions;
3. Quick Settings Tile, nếu phù hợp với phiên bản Android và khả năng triển khai.

Mục tiêu:

```text
Không cần mở App
      ↓
Chọn Activity
      ↓
1 tap
      ↓
Session được ghi nhận
```

---

### X.X.7. Android Widget

#### FR-LF-06 — Quick Tracking Widget

Ứng dụng MUST cung cấp Widget cho phép người dùng thực hiện các thao tác Tracking phổ biến trực tiếp từ Home Screen.

Widget SHOULD hỗ trợ:

* hiển thị các Activity thường sử dụng;
* Start Activity bằng một tap;
* Stop Activity đang chạy bằng một tap;
* hiển thị Activity đang active;
* hiển thị thời lượng hiện tại;
* cập nhật trạng thái theo dữ liệu thực tế;
* hỗ trợ nhiều Activity đang chạy đồng thời;
* hỗ trợ kích thước Widget khác nhau tùy khả năng của Android.

Widget MUST NOT yêu cầu người dùng mở ứng dụng chỉ để thực hiện một thao tác Start/Stop thông thường.

---

### X.X.8. Widget — Compact Mode

Widget kích thước nhỏ SHOULD ưu tiên các Activity người dùng sử dụng thường xuyên.

Ví dụ:

```text
┌──────────────────────┐
│ TimeLens             │
│                      │
│ [ Coding ] [ Study ] │
│ [ Work  ] [ TikTok ] │
└──────────────────────┘
```

Một tap vào Activity:

```text
[ Coding ]
     ↓
Start Coding
```

Nếu Activity đang chạy:

```text
[ Coding 01:24 ]
     ↓
Stop Coding
```

---

### X.X.9. Widget — Active Sessions

Widget kích thước lớn SHOULD hiển thị các session đang hoạt động.

Ví dụ:

```text
┌────────────────────────────┐
│ TimeLens          Today    │
│                            │
│ ● Coding       01:24:32    │
│   [ STOP ]                 │
│                            │
│ ● Reading      00:42:15    │
│   [ STOP ]                 │
│                            │
│ [ Study ] [ Work ]         │
└────────────────────────────┘
```

Nếu không có Activity đang chạy:

```text
┌────────────────────────────┐
│ TimeLens          Today    │
│                            │
│      Nothing running       │
│                            │
│ [ Coding ] [ Study ]       │
│ [ Work ]   [ Other ]       │
└────────────────────────────┘
```

---

### X.X.10. Timer không phụ thuộc vào việc App đang chạy liên tục

#### FR-LF-07 — Timestamp-based Tracking

Hệ thống MUST NOT phụ thuộc vào một timer process chạy liên tục để xác định thời lượng Activity.

Thời lượng phải được tính từ:

```text
duration = endedAt - startedAt
```

Đối với session đang chạy:

```text
duration = currentTime - startedAt
```

Do đó, nếu ứng dụng bị đưa xuống background hoặc process của ứng dụng bị hệ thống Android dừng, dữ liệu Tracking vẫn có thể được xác định dựa trên timestamp đã lưu.

Widget và UI phải tính toán thời lượng từ timestamp thay vì yêu cầu một background timer chạy liên tục.

---

### X.X.11. Tự động hóa các thao tác không cần người dùng can thiệp

Các tác vụ không liên quan trực tiếp đến việc ghi nhận Activity SHOULD được hệ thống tự động thực hiện:

| Tác vụ                        | Cơ chế      |
| ----------------------------- | ----------- |
| Tính duration                 | Tự động     |
| Tính overlap                  | Tự động     |
| Daily analytics               | Tự động     |
| Firebase synchronization      | Tự động     |
| Google Drive backup           | WorkManager |
| Phát hiện dữ liệu cần sync    | Tự động     |
| Xử lý retry khi network lỗi   | Tự động     |
| Cập nhật Widget               | Tự động     |
| Cập nhật Active Session state | Tự động     |

Người dùng chỉ cần can thiệp khi:

* tạo Activity mới;
* chỉnh sửa dữ liệu;
* giải quyết conflict nếu cần;
* thay đổi Settings;
* thực hiện Restore;
* thực hiện các thao tác quản trị dữ liệu.

---

### X.X.12. UX Performance Target

Hệ thống SHOULD hướng tới các mục tiêu sau:

| Metric                   | Target                          |
| ------------------------ | ------------------------------- |
| Start Activity từ App    | ≤ 1 user action                 |
| Stop Activity từ App     | ≤ 1 user action                 |
| Start Activity từ Widget | ≤ 1 user action                 |
| Stop Activity từ Widget  | ≤ 1 user action                 |
| Start/Stop thông thường  | Không yêu cầu form              |
| Start/Stop thông thường  | Không yêu cầu confirmation      |
| Start/Stop thông thường  | Không yêu cầu network           |
| Start/Stop thông thường  | Không yêu cầu Firebase response |
| Start/Stop thông thường  | Không yêu cầu Google Drive      |
| Tracking khi offline     | MUST hoạt động                  |

---

### X.X.13. Nguyên tắc ưu tiên

Khi thiết kế một chức năng mới, hệ thống phải ưu tiên theo thứ tự:

```text
1. Ghi nhận được hành động thực tế
            ↓
2. Ghi nhận nhanh
            ↓
3. Không làm gián đoạn người dùng
            ↓
4. Tự động xử lý dữ liệu
            ↓
5. Cho phép chỉnh sửa sau
            ↓
6. Thu thập metadata bổ sung
```

Không được hy sinh khả năng ghi nhận nhanh chỉ để thu thập thêm metadata không bắt buộc.

### X.X.14. Nguyên tắc tổng quát

Toàn bộ ứng dụng phải tuân thủ nguyên tắc:

> **The user should spend as little time as possible recording their time.**

Ứng dụng tồn tại để quan sát và phân tích hành vi thực tế của người dùng, không biến việc Tracking thành một công việc mà người dùng phải liên tục quản lý.

**Core interaction:**

```text
DO SOMETHING
     ↓
1 TAP
     ↓
SYSTEM RECORDS
     ↓
SYSTEM CALCULATES
     ↓
SYSTEM SYNCS / BACKUPS
     ↓
USER REVIEWS WHEN NEEDED
```
