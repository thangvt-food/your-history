# Session 4: Tích hợp VietQR Payment Deeplink & Trải nghiệm 1 Chạm Chuyển tiền (2026-09-24)

## 1. Bối cảnh & Vấn đề
- Người dùng phản hồi: "tại sao tôi phải scan qr 2 lần cho 1 giao dịch nhỉ?? tạo VietQR payment deeplink → mở MB... không muốn dùng kiểu chuyền trung gian này vì nó đã xóa bỏ đi ý nghĩa tồn tại của app này".
- Luồng cũ: Quét VietQR -> Nhập tiền/memo -> Bấm "Chuyển nhanh & Lưu" -> Lưu ảnh QR vào Gallery -> Hiện màn hình hướng dẫn người dùng mở MB Bank và chọn ảnh từ Thư viện để quét lại -> Trải nghiệm bị trùng lặp thao tác quét (Double Scan).

## 2. Giải pháp Kỹ thuật
- **Nghiên cứu chuẩn VietQR Payment Deeplink**:
  - Dựa trên API công khai `https://api.vietqr.io/v2/android-app-deeplinks`, MB Bank hỗ trợ deeplink với `appId: "mb"` và cờ `autofill: 1`.
  - Cấu trúc Payment Deeplink chuẩn:
    `https://dl.vietqr.io/pay?app={appId}&ba={accountNumber}@{bankBin}&am={amount}&tn={memo}&bn={recipientName}`
  - Khi mở qua Android Intent (`ACTION_VIEW`), hệ thống điều hướng trực tiếp sang intent của MB Bank (`intent://#Intent;scheme=mbbank;package=com.mbmobile;end`), tự động điền thông tin người nhận.
- **Cập nhật `BankInfo.kt`**:
  - Bổ sung `vietQrAppId` và `scheme` cho các ngân hàng tại Việt Nam (MBBank `mb`/`mbbank`, Vietcombank `vcb`/`vietcombank`, Techcombank `tcb`/`techcombank`, VietinBank `icb`/`vietinbankipay`, BIDV `bidv`, ACB `acb`/`acbone`, v.v.).
- **Nâng cấp `BankingHandoffManager.kt`**:
  - Bổ sung hàm `buildVietQrPaymentDeeplink(...)`.
  - Hàm `launchBankingHandoff(...)` ưu tiên mở Payment Deeplink của app ngân hàng người dùng sở hữu (ưu tiên MBBank).
  - Tự động sao chép STK, số tiền, nội dung vào Clipboard trước khi mở app để dự phòng 100%.
  - Fallback sang package launcher nếu máy không mở được deeplink URL.
- **Tối ưu UX trên `TransactionFormScreen.kt`**:
  - Nút hành động chính: **"Mở [Tên Bank] chuyển tiền & Lưu"** (ví dụ: "Mở MBBank chuyển tiền & Lưu").
  - 1 Chạm: Chuyển khoản qua MB Bank + Tự động lưu chi tiêu vào Room Database + Hoàn tất giao dịch.
  - Loại bỏ hoàn toàn bước trung gian "Lưu ảnh và mở bank quét lại ảnh".
  - Nút phụ: "Chỉ lưu chi tiêu (không mở app)".
  - Nút tùy chọn: "Hiện mã QR cho thiết bị khác quét" (chỉ kích hoạt khi người dùng muốn đưa máy cho người khác quét).
- **Unit Test**:
  - Bổ sung `BankingHandoffTest.kt` kiểm thử việc dựng URL VietQR Payment Deeplink và mapping ngân hàng.

## 3. Danh sách File thay đổi
- `app/src/main/java/com/yourhistory/app/domain/model/BankInfo.kt`
- `app/src/main/java/com/yourhistory/app/domain/handoff/BankingHandoffManager.kt`
- `app/src/main/java/com/yourhistory/app/ui/transaction/TransactionFormScreen.kt`
- `app/src/main/java/com/yourhistory/app/ui/transaction/TransactionFormViewModel.kt`
- `app/src/test/java/com/yourhistory/app/BankingHandoffTest.kt`
- `AGENTS.md`
- `docs/sessions/2026-09-24-vietqr-deeplink.md`
