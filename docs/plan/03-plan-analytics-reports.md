# Plan 03 — Analytics & Reports

**Status:** `DONE`

## Goal
Hoàn thiện khả năng báo cáo đa chiều (Day/Week/Month) theo đúng triết lý "Visualize reality".

## Dependencies
Plan 02

## Phases

### Phase 1: Daily Activity Breakdown (DONE)

#### Objective
Tính tổng và liệt kê các Activity theo duration trong 1 ngày.

#### Current Evidence
- `AnalyticsEngine` đã có thuật toán tính overlap.
- `AnalyticsScreen` mới vẽ ring chart.

#### Proposed Changes
- Thêm logic tính toán `Map<Activity, Duration>` cho ngày.
- Hiện trên `AnalyticsScreen`.

---

### Phase 2: Weekly Analytics (DONE)

#### Objective
Hiển thị báo cáo 7 ngày (tuần).

#### SRS Requirements
- REQ: Weekly Analytics.

#### Proposed Changes
- Nâng cấp `AnalyticsEngine` -> `calculateWeeklyAnalytics()`.
- Xây dựng UI `WeeklyBarChart` (hiển thị xu hướng 7 ngày).

---

### Phase 3: Monthly Analytics (DONE)

#### Objective
Hiển thị báo cáo tháng.

#### SRS Requirements
- REQ: Monthly Analytics.

#### Proposed Changes
- Nâng cấp `AnalyticsEngine` -> `calculateMonthlyAnalytics()`.
- Xây dựng UI Lịch/Heatmap hoặc Bar chart tháng.

---

### Phase 4: Activity Relationships (DONE)

#### Objective
Thống kê cơ bản "Activity A × Activity B" overlap.

#### Proposed Changes
- Bổ sung UI RelationshipStats trên Analytics screen.
- Lọc các cặp session song song bằng thuật toán tập hợp.

#### Tests
- `Unit Test` cho tính toán Weekly/Monthly đảm bảo tuân thủ cấu hình Day Boundary (từ Plan 01).
