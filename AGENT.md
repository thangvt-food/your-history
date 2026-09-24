# AGENTS.md - Project Context, Technical Standards & Working Conventions

Tài liệu này là "nguồn sự thật duy nhất" (Single Source of Truth) cho AI Agent và lập trình viên trong quá trình phát triển dự án **Your History** (Ứng dụng quản lý chi tiêu cá nhân trên Android).

---

## 1. Tổng quan dự án (Project Overview)
- **Tên dự án**: Your History
- **Mục tiêu**: Ứng dụng Android quản lý tài chính cá nhân tối ưu hóa cho hành vi thanh toán tại Việt Nam:
  - Quét & lưu thông tin VietQR để chuyển khoản nhanh (hỗ trợ cả Camera trực tiếp và chọn ảnh từ Thư viện).
  - Tự động gán danh mục & nội dung chuyển khoản định sẵn, tránh gõ lại mục đích trên app ngân hàng.
  - Tự động lưu vết lịch sử chi tiêu kèm thông tin người nhận / mã QR.
  - Cơ chế Handoff sang Ngân hàng: Ưu tiên Deep Link Napas VietQR (`vietqr://transfer?...`); fallback cho phép chọn app ngân hàng cụ thể và tự động sao chép STK, số tiền, nội dung vào Clipboard.
  - Hỗ trợ cả Chi tiêu (Expense) và Thu nhập (Income), trọng tâm là luồng Chi tiêu qua QR.
  - Lưu trữ dữ liệu cục bộ (Local-first, Room Database), sẵn sàng đồng bộ Cloud trong tương lai.
  - CI/CD bằng GitHub Actions để build APK cho máy cấu hình yếu.

---

## 2. Quy ước Kỹ thuật & Phiên bản Cố định (Tech Stack & Pinned Versions)
Toàn bộ mã nguồn phải tuân thủ nghiêm ngặt các phiên bản đã được chốt (khớp 100% với `build.gradle.kts` và CI workflow):

| Thành phần | Phiên bản | Ghi chú |
| :--- | :--- | :--- |
| **Android Target / Compile SDK** | `34` | Android 14 |
| **Android Min SDK** | `26` | Android 8.0+ |
| **Android Gradle Plugin (AGP)** | `8.4.2` | Plugin com.android.application |
| **Gradle Distribution** | `8.7` | `gradle-8.7-bin.zip` |
| **JDK** | `17` | Eclipse Temurin distribution |
| **Kotlin Language** | `1.9.24` | JVM Target 17 |
| **Kotlin KSP** | `1.9.24-1.0.20` | Dùng cho Room annotation processor |
| **Compose Compiler Extension** | `1.5.14` | Tương thích Kotlin 1.9.24 |
| **Compose BOM** | `2024.06.00` | Quản lý toàn bộ thư viện Compose UI & Material 3 |
| **Navigation Compose** | `2.7.7` | Điều hướng giữa các Screen |
| **Dependency Injection** | `Koin 3.5.6` | `koin-androidx-compose` (nhẹ, build nhanh, không tốn tài nguyên kapt/ksp) |
| **Cơ sở dữ liệu quan hệ** | `Room 2.6.1` | `room-runtime`, `room-ktx`, `room-compiler` |
| **Lưu trữ Cài đặt / Preferences** | `DataStore 1.1.1` | `androidx.datastore:datastore-preferences` |
| **Bất đồng bộ (Asynchronous)** | `Coroutines 1.8.1` | `kotlinx-coroutines-android` + StateFlow / SharedFlow |
| **Camera Preview** | `CameraX 1.3.4` | `camera-camera2`, `camera-lifecycle`, `camera-view` |
| **Nhận diện mã vạch** | `ML Kit 17.2.0` | `com.google.mlkit:barcode-scanning` (100% offline) |
| **Dựng mã QR** | `ZXing core 3.5.3` | `com.google.zxing:core` (thuần Java, render VietQR cho app bank quét) |

---

## 3. Quy ước Cấu trúc Gói (Package Structure Conventions)

Dự án áp dụng mô hình phân tách gói theo lớp kết hợp tính năng:

```
com.yourhistory.app/
├── YourHistoryApp.kt             # Application class khởi tạo Koin
├── MainActivity.kt               # Single Activity, Edge-to-Edge, Compose root
├── di/                           # Koin dependency injection modules
│   └── AppModule.kt
├── data/                         # Tầng Dữ liệu (Local-first)
│   ├── local/
│   │   ├── entity/               # Room Entities (Category, QrContact, Transaction)
│   │   ├── dao/                  # Room DAOs (CategoryDao, QrContactDao, TransactionDao)
│   │   ├── preferences/          # DataStore Preferences (UserPreferencesRepository)
│   │   └── AppDatabase.kt        # Room Database & migrations/seeds
│   └── repository/               # Repository interfaces & implementations
├── domain/                       # Tầng Nghiệp vụ cốt lõi (Pure Kotlin preferred)
│   ├── model/                    # Domain models (BankInfo, VietQrData, FinancialSummary)
│   ├── parser/                   # VietQrParser, VietQrBuilder (parse & dựng EMVCo)
│   └── handoff/                  # BankingHandoffManager (Deep links & app chooser)
└── ui/                           # Tầng Giao diện người dùng (Jetpack Compose)
    ├── home/                     # Màn hình Trang chủ & Dashboard thu chi
    ├── scanner/                  # Màn hình Quét VietQR (CameraX + Gallery picker)
    ├── transaction/              # Màn hình Nhập chi tiêu & Form chuyển khoản
    ├── showqr/                   # Màn hình Hiện VietQR cho app bank quét (ZXing render)
    ├── contacts/                 # Màn hình Quản lý Danh bạ QR chuyển nhanh
    ├── history/                  # Màn hình Lịch sử & Bộ lọc giao dịch
    ├── navigation/               # AppNavigation & Screen route definitions
    └── theme/                    # Material 3 Color, Type, Theme
```

---

## 4. Quy ước Bảo mật & Dữ liệu Nhạy cảm (PII & Data Minimization)
- **Định danh thông tin PII**: Số tài khoản ngân hàng (STK), số tiền giao dịch, tên người nhận, và nội dung chuyển khoản là dữ liệu nhạy cảm cá nhân.
- **Tối thiểu hóa Dữ liệu (PII Minimization)**:
  - Tuyệt đối không lưu trữ chuỗi mã QR thô (`rawPayload` / `rawQrPayload`) vào cơ sở dữ liệu Room. Chuỗi thô chỉ được xử lý tạm thời trên bộ nhớ RAM (in-memory) trong phiên quét và giải phóng ngay sau khi trích xuất xong các trường cấu trúc (`bankBin`, `accountNumber`).
  - Không thu thập hoặc lưu vết bất kỳ thông tin nào không phục vụ trực tiếp cho mục đích thanh toán và quản lý chi tiêu.
- **Quy tắc ghi Log**:
  - Tuyệt đối không log thông tin STK thô, số tiền hoặc ghi chú giao dịch ra `android.util.Log` hoặc Logcat.
  - Trong trường hợp cần debug lỗi parse QR, chỉ log mã lỗi hoặc độ dài chuỗi, không log toàn bộ payload thô.
- **Lưu trữ Cục bộ an toàn**:
  - Toàn bộ cơ sở dữ liệu Room (`your_history.db`) và DataStore (`user_preferences.preferences_pb`) chỉ được lưu tại bộ nhớ trong (Internal App Storage: `/data/user/0/com.yourhistory.app/...`).
  - Không lưu trữ tệp dữ liệu tài chính vào bộ nhớ ngoài (External Storage) dùng chung.
- **Bảo vệ Clipboard**:
  - Khi sao chép thông tin chuyển khoản vào Clipboard, phải thông báo rõ ràng cho người dùng qua Toast/Snackbar.
- **Quyền riêng tư**:
  - Không truyền tải dữ liệu tài chính người dùng qua bất kỳ API analytics của bên thứ ba nào.

---

## 5. Tiêu chí Hoàn thành (Definition of Done - DoD)
Một tính năng hoặc thay đổi chỉ được coi là hoàn thành khi đáp ứng đủ các tiêu chí sau:
1. **Biên dịch & Kiểm thử (Local vs CI)**:
   - **Local Verification**: `./gradlew assembleDebug test --no-daemon` chạy thành công 100%, toàn bộ unit test đều pass trước khi commit/push.
   - **CI Build**: Workflow trên GitHub Actions chạy thành công với lệnh tối ưu `./gradlew assembleDebug --no-daemon -x test -x lint -x lintVitalAnalyzeRelease` để sinh ra APK trong thời gian ngắn nhất.
2. **Kiểm thử tự động**: Toàn bộ Unit test trong `app/src/test/` đều pass 100%. Các module bóc tách dữ liệu mới bắt buộc phải có Unit test đi kèm.
3. **Đặc tả tính năng (Feature Spec)**: Phải có file mô tả trong thư mục `docs/specs/<feature-name>.md` tuân thủ đủ 5 mục chuẩn trước khi tiến hành code.
4. **Kiến trúc**: Tuân thủ Clean Architecture + MVVM. Tầng UI không được gọi trực tiếp DAO, mọi truy cập dữ liệu phải thông qua Repository và ViewModel.
5. **Kiểm tra PII & Data Minimization**: Không có dữ liệu tài chính nhạy cảm bị rò rỉ vào Logcat và không lưu payload QR thô vào Database.
6. **Nhật ký Session**: Cập nhật file chi tiết trong `docs/sessions/YYYY-MM-DD-<title>.md` và chỉ ghi nhận bài học kinh nghiệm cốt lõi tại `AGENTS.md`.

---

## 6. Quy ước CI/CD & Build Tối ưu (GitHub Actions)
- **Runner**: `ubuntu-latest`.
- **Triggers**:
  - `push` vào nhánh `main`.
  - `pull_request` vào nhánh `main`.
  - `workflow_dispatch` (kích hoạt thủ công).
- **Concurrency**: `group: ${{ github.workflow }}-${{ github.ref }}`, `cancel-in-progress: true` (hủy bỏ build cũ khi có commit mới để tiết kiệm phút runner).
- **Cấu hình tối ưu**:
  - Sử dụng `gradle/actions/setup-gradle@v4` với `gradle-home-cache-cleanup: true`.
  - Command: `./gradlew assembleDebug --no-daemon -x test -x lint -x lintVitalAnalyzeRelease`.
  - JVM args: `-Xmx2048m -XX:+UseG1GC`, `parallel=true`, `caching=true`, `vfs.watch=false`, `kotlin.incremental=false`.
- **Artifact đầu ra**: `your-history-debug-apk` chứa `app-debug.apk`, thời gian lưu trữ (retention) 14 ngày.

---

## 7. Bài học Kinh nghiệm Cốt lõi (Core Lessons Learned)

### Session 1: Khởi tạo Toàn diện Dự án MVP & CI/CD (2026-09-24)
- **Tài liệu chi tiết**: Xem đầy đủ kế hoạch tại [`docs/plans/plan_expense_manager.md`](docs/plans/plan_expense_manager.md) và chi tiết changelog/file tại [`docs/sessions/2026-09-24-init.md`](docs/sessions/2026-09-24-init.md).
- **Kinh nghiệm kỹ thuật**:
  1. *Handoff sang App Ngân hàng tại VN*: Các app ngân hàng tại Việt Nam không có một chuẩn intent thống nhất để tự động điền form chuyển khoản ngoại trừ chuẩn Napas 247 VietQR (`vietqr://transfer?...`). Do đó giải pháp kết hợp Deep Link + tự động sao chép thông tin vào Clipboard + danh sách app ngân hàng đã cài trên máy là cơ chế an toàn và thuận tiện nhất.
  2. *Thiết kế Parser độc lập*: Không phụ thuộc vào `android.net.Uri` trong parser mà dùng `java.net.URI` thuần JVM giúp cho việc chạy Unit test nhanh hơn hàng chục lần mà không cần giả lập môi trường Android.
   3. *Tách biệt Doc và Changelog*: Giữ `AGENTS.md` tập trung vào Quy ước, Tiêu chuẩn kỹ thuật (Source of Truth) và Definition of Done. Chuyển chi tiết danh sách file và nhật ký phiên làm việc ra thư mục `docs/sessions/` để tránh tài liệu bị phình to mất kiểm soát.

### Session 2: Handoff v2 & UX Nhập tiền/Tag (2026-09-24)
- **Tài liệu chi tiết**: [`docs/sessions/2026-09-24-handoff-ux.md`](docs/sessions/2026-09-24-handoff-ux.md), spec §4.2–4.3 tại [`docs/specs/vietqr-expense-tracking.md`](docs/specs/vietqr-expense-tracking.md).
- **Kinh nghiệm kỹ thuật**:
  1. *Deep link có package vẫn là best-effort*: `Intent(ACTION_VIEW, vietqrUri).setPackage(pkg)` chỉ hiệu quả nếu bank đăng ký scheme Napas; phần lớn trường hợp vẫn mở trắng nên phải Toast trung thực + hướng dẫn dán từ Clipboard thay vì hứa hẹn tự điền.
  2. *Tiền nhập digits-only, hiển thị grouped*: giữ `amountText` là chữ số thô trong ViewModel, format `1 000 000` ở tầng UI bằng hàm thuần Kotlin trong `companion object` để dễ unit test JVM.

### Session 3: Luồng Hiện VietQR để quét (2026-09-24)
- **Tài liệu chi tiết**: [`docs/sessions/2026-09-24-show-qr.md`](docs/sessions/2026-09-24-show-qr.md) (bản đầu, đã bị thay thế), luồng đúng tại [`docs/sessions/2026-09-24-gallery-qr.md`](docs/sessions/2026-09-24-gallery-qr.md), spec §4.4 tại [`docs/specs/vietqr-expense-tracking.md`](docs/specs/vietqr-expense-tracking.md).
- **Kinh nghiệm kỹ thuật**:
  1. *Không có API public để điền sẵn app bank*: deeplink (kể cả `mbbank://`) chỉ mở app; Zalo làm được nhờ hợp tác riêng + ký số. Hiện QR trên màn hình cũng vô dụng với 1 máy. Luồng universal duy nhất: dựng QR động -> lưu ảnh Thư viện -> quét từ ảnh trong app bank (VCB/TCB có tài liệu chính thức).
  2. *Chuẩn hóa ASCII trước khi dựng EMV*: tên/memo bỏ dấu, in hoa, truncate theo giới hạn TLV để tránh mã QR lỗi mà bank không đọc được.
