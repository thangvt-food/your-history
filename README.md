# Your History 📱💸

> **Ứng dụng Quản lý Chi tiêu Cá nhân & Hỗ trợ Chuyển khoản VietQR Nhanh trên Android**

[![Build Debug APK](https://github.com/thangvt-food/your-history/actions/workflows/build-apk.yml/badge.svg)](https://github.com/thangvt-food/your-history/actions/workflows/build-apk.yml)

---

## 🌟 Điểm nổi bật & Vấn đề giải quyết

1. **Quét & Phân tích VietQR Đa nguồn**:
   - Quét mã QR trực tiếp qua Camera (sử dụng Google ML Kit Barcode Scanning offline).
   - Chọn ảnh chụp màn hình mã QR từ Thư viện ảnh (Gallery).
   - Tự động bóc tách chuẩn xác mã BIN ngân hàng, số tài khoản, số tiền và nội dung theo chuẩn Napas / EMVCo.

2. **Chuyển tiền qua Ngân hàng mà không tốn công gõ lại nội dung**:
   - Cho phép định sẵn nội dung và danh mục (Ăn trưa, Cà phê, Mua sắm, Tiền phòng...).
   - Bấm **"Chuyển tiền & Lưu chi tiêu"**: Ứng dụng tự động lưu vào sổ chi tiêu trên máy, đồng thời gọi Deep Link Napas VietQR hoặc mở trực tiếp app ngân hàng đã cài đặt trên điện thoại.
   - **Cơ chế Fallback thông minh**: Tự động sao chép số tài khoản, số tiền và nội dung vào Clipboard phòng khi app ngân hàng không tự nhận tham số intent.

3. **Danh bạ QR Chuyển nhanh 1 chạm**:
   - Lưu lại người nhận quen thuộc (bạn bè, quán ăn quen, chủ nhà).
   - Lần sau chuyển khoản chỉ cần bấm vào tên người nhận trên màn hình chính, không cần tìm lại ảnh mã QR.

4. **Lưu trữ Cục bộ (Local-first) & Sẵn sàng Cloud**:
   - Dữ liệu lưu an toàn 100% trên máy bằng **Room Database**.
   - Cấu trúc Entity thiết kế sẵn các trường đồng bộ hóa (`id: UUID`, `createdAt`, `updatedAt`, `isDeleted`, `syncStatus`) giúp dễ dàng kết nối Cloud (Firebase/Supabase) sau này.

5. **CI/CD Tối ưu Tốc độ Build (GitHub Actions)**:
   - Tự động build và xuất file `app-debug.apk` trong phần **Actions Artifacts**.
   - Tối ưu bộ nhớ JVM, tắt daemon và bỏ qua các task không cần thiết để build nhanh nhất cho người dùng không có máy tính mạnh.

---

## 🏗️ Kiến trúc & Công nghệ (Tech Stack)

- **Language**: Kotlin 1.9
- **UI Framework**: Jetpack Compose + Material 3 (Edge-to-Edge, Dark & Light Mode)
- **Architecture**: MVVM + Clean Architecture nhẹ nhàng
- **Local Storage**: Room Database + Coroutines Flow (StateFlow)
- **Dependency Injection**: Koin
- **Camera & Scanner**: CameraX + Google ML Kit Barcode Scanning
- **Target SDK**: Android 14 (API 34), Min SDK: Android 8.0 (API 26)

---

## 🚀 Cách tải & cài đặt file APK từ GitHub Actions

1. Vào tab **[Actions](https://github.com/thangvt-food/your-history/actions)** trên repository GitHub.
2. Chọn lần chạy mới nhất của workflow **Build Debug APK**.
3. Kéo xuống mục **Artifacts** ở dưới cùng trang.
4. Tải file `your-history-debug-apk.zip` về, giải nén ra file `app-debug.apk`.
5. Cài đặt trực tiếp lên điện thoại Android của bạn!

*(Bạn cũng có thể bấm nút **Run workflow** trong tab Actions bất kỳ lúc nào để build ra bản APK mới nhất mà không tốn tài nguyên máy cá nhân).*

---

## 📁 Cấu trúc Thư mục

```
your-history/
├── .github/workflows/
│   └── build-apk.yml               # GitHub Actions build APK tối ưu
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml     # Khai báo quyền Camera, queries app ngân hàng
│   │   ├── java/com/yourhistory/app/
│   │   │   ├── data/               # Local-first Room Database, Entities, DAOs, Repo
│   │   │   ├── domain/             # VietQR parser, Model ngân hàng VN, Banking Handoff
│   │   │   ├── ui/                 # Jetpack Compose UI (Home, Scanner, Form, History)
│   │   │   └── di/                 # Koin AppModule
│   │   └── res/                    # Icon, themes, colors, XML config
│   └── build.gradle.kts
├── docs/
│   ├── plans/
│   │   └── plan_expense_manager.md # Kế hoạch triển khai kiến trúc
│   ├── specs/
│   │   └── vietqr-expense-tracking.md # Tài liệu đặc tả kỹ thuật chi tiết
│   └── sessions/
│       └── 2026-09-24-init.md      # Chi tiết thay đổi & session log
├── AGENTS.md                       # Source of Truth: Quy ước, Pinned Versions, DoD
└── README.md
```

---

## 📝 Quy ước Phát triển
Xem chi tiết tại file [AGENTS.md](AGENTS.md) về kiến trúc, các phiên bản cố định, quy ước PII và Definition of Done.
