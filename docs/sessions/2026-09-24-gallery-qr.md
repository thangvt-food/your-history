# Session Log: 2026-09-24 - Chuyển nhanh qua ảnh QR Thư viện (sửa sai luồng Hiện QR)

- **Vấn đề người dùng báo**: luồng "hiện QR cho app bank quét" làm ngược yêu cầu —
  mục đích là quét để chuyển NHANH + lưu lịch sử, không phải thêm bước.
  Người dùng tin "phải có cách" (API) để điền sẵn vào app bank.

## 1. Kết quả nghiên cứu (web, có nguồn)

- Tài liệu vietqr.io (`/danh-sach-api/deeplink-app-ngan-hang`): deeplink
  `dl.vietqr.io/pay?app=...` **chỉ mở được app, chưa thể tự động điền**
  STK/số tiền (kể cả scheme riêng `mbbank://` của MB Bank).
- Dev VN (VOZ): "trừ quét QR trong bank thì chỉ có Zalo mở được, vì nó làm việc
  với bank" (hợp tác riêng + ký số, private key) — **không có API public**.
- Tài liệu chính thức VCB Digibank + Techcombank Mobile: màn hình quét hỗ trợ
  **"chọn mã QR từ thư viện ảnh"** — đây là luồng universal cho 1 điện thoại.
- Sai lầm đã nhận: hiện QR trên màn hình vô dụng với 1 máy (camera không quét
  được màn hình của chính nó).

## 2. Thay đổi

- Mới `ui/showqr/QrImageSaver.kt`: lưu PNG vào `Pictures/YourHistory` qua
  MediaStore (Android 10+ không cần quyền); Android 8-9 xin
  `WRITE_EXTERNAL_STORAGE` (manifest `maxSdkVersion=28`, xin runtime ở form).
- `TransactionFormViewModel.buildAndShowQr(context, senderPkg)`: build payload
  -> render -> lưu ảnh -> `persistTransaction` -> copy clipboard dự phòng ->
  event `ShowQr` mở rộng (bankBin, senderPackage/Name, imageSaved).
- `TransactionFormScreen`: nút chính "Chuyển nhanh & Lưu" (chọn bank gửi nếu
  >1 app); bottom sheet đổi thành chọn tài khoản chuyển đi + giải thích luồng ảnh.
- `ShowQrScreen` viết lại: preview ảnh đã lưu + tóm tắt đối chiếu + 4 bước
  (mở bank -> quét QR -> ảnh từ thư viện -> xác nhận) + nút "Mở {bank} ngay"
  (`BankingHandoffManager.openAppPackage` mới, không copy lại clipboard).
- Route `ShowQr` thêm args bankBin/senderPkg/senderName/imgSaved (encode đầy đủ).

## 3. Kiểm thử

- Chưa biên dịch local (không có Android SDK) — cần CI xác minh.
- Cần manual test trên máy thật: bấm Chuyển nhanh -> ảnh lưu Thư viện ->
  mở MB Bank -> quét từ ảnh -> STK/tiền/nội dung tự điền.
- Tái sử dụng `VietQrBuilder`/`QrBitmapRenderer`/`VietQrBuilderTest` từ session trước.
