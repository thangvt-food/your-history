# Feature Spec: Quản lý Chi tiêu & Chuyển khoản VietQR Nhanh

## 1. Mục tiêu (Objective & User Story)
- **Bài toán**: Người dùng tại Việt Nam thường xuyên thanh toán chuyển khoản qua mã VietQR (ăn trưa, mua hàng, trả nợ người quen). Mỗi lần chuyển đều phải mở app ngân hàng, quét QR, và tự nhập nội dung mục đích chi tiêu hoặc không ghi chép lại được lịch sử vào sổ chi tiêu.
- **User Story**:
  - Là người dùng cá nhân, tôi muốn quét mã VietQR bằng Camera hoặc chọn ảnh QR từ Thư viện ngay trong app "Your History".
  - App tự động trích xuất thông tin người nhận, số tài khoản, ngân hàng, và số tiền (nếu có).
  - Tôi có thể chọn Danh mục chi tiêu (Cơm trưa, Cà phê, Siêu thị...) và định sẵn nội dung chuyển khoản.
  - Khi tôi bấm "Chuyển tiền & Lưu", app sẽ:
    1. Tự động lưu bản ghi chi tiêu vào thiết bị (Local-first).
    2. Gọi Deep Link Napas VietQR hoặc mở trực tiếp app ngân hàng đã cài đặt trên máy.
    3. Tự động copy số tài khoản, số tiền và nội dung vào Clipboard phòng khi app ngân hàng không tự nhận tham số.
  - Tôi có thể lưu người nhận vào Danh bạ QR để lần sau chuyển tiền chỉ cần 1 chạm.

---

## 2. Mô hình Dữ liệu (Data Model)
Các Entity lưu trữ trong Room Database:

### 2.1. `CategoryEntity` (`categories`)
- `id: String` (UUID) - Primary Key
- `name: String` (VD: "Ăn uống", "Đi lại", "Mua sắm", "Lương", "Khác")
- `type: String` ("EXPENSE" hoặc "INCOME")
- `icon: String` (Tên icon Material)
- `colorHex: String` (Mã màu hiển thị)
- `isDefault: Boolean` (true nếu là danh mục mặc định hệ thống)
- `createdAt: Long`, `updatedAt: Long`, `isDeleted: Boolean`, `syncStatus: String`

### 2.2. `QrContactEntity` (`qr_contacts`)
- `id: String` (UUID) - Primary Key
- `recipientName: String` (Tên người nhận)
- `bankBin: String` (Mã BIN 6 số, VD: 970422 cho MB, 970436 cho VCB)
- `bankName: String` (Tên viết tắt, VD: "MBBank", "Vietcombank")
- `accountNumber: String` (Số tài khoản nhận)
- `defaultAmount: Long?` (Số tiền định sẵn nếu có)
- `defaultNote: String` (Nội dung chuyển khoản mặc định)
- `defaultCategoryId: String?` (Danh mục mặc định gắn với người nhận này)
- `lastUsedAt: Long` (Timestamp dùng gần nhất để sắp xếp)
- `createdAt: Long`, `updatedAt: Long`, `isDeleted: Boolean`, `syncStatus: String`

### 2.3. `TransactionEntity` (`transactions`)
- `id: String` (UUID) - Primary Key
- `amount: Long` (Số tiền VNĐ)
- `type: String` ("EXPENSE" hoặc "INCOME")
- `categoryId: String` (Foreign Key tới Category)
- `note: String` (Nội dung chi tiêu / chuyển khoản)
- `qrContactId: String?` (Liên kết tới QR Contact nếu có)
- `recipientName: String?` (Tên người nhận)
- `bankName: String?` (Ngân hàng nhận)
- `accountNumber: String?` (Số tài khoản nhận)
- `transactionDate: Long` (Thời gian giao dịch)
- `paymentMethod: String` ("VIETQR", "CASH", "BANK_TRANSFER")
- `createdAt: Long`, `updatedAt: Long`, `isDeleted: Boolean`, `syncStatus: String`

---

## 3. UI/UX Flow & Wireframe

### Luồng Quét QR & Ghi chép
1. **Home Screen**:
   - Thẻ thống kê: Tổng chi, tổng thu, số dư trong tháng.
   - Nút nổi bật: **Quét VietQR** (Floating Action Button hoặc Banner to).
   - Danh sách "Chuyển tiền nhanh 1 chạm" (Hiển thị các QR Contact vừa dùng).
   - Danh sách giao dịch gần đây.
2. **Scanner Screen**:
   - Màn hình Camera toàn màn hình với khung quét hình vuông ở giữa.
   - Nút bật/tắt đèn Flash.
   - Nút "Chọn ảnh từ Thư viện" để tải ảnh QR chụp màn hình.
3. **Transaction Edit / Confirmation Screen**:
   - Hiển thị thông tin ngân hàng & số tài khoản đã bóc tách từ QR.
   - Ô nhập số tiền (nếu QR tĩnh chưa có tiền, hoặc sửa số tiền).
   - Lưới chọn nhanh Danh mục (Ăn uống, Tiêu vặt, Mua sắm...).
   - Ô nội dung chuyển khoản (có gợi ý nhanh hoặc lấy từ mẫu).
   - Checkbox: "Lưu vào danh bạ chuyển nhanh".
   - 2 nút hành động:
     - "Chuyển tiền qua Ngân hàng & Lưu": Tự động copy clipboard + mở App Ngân hàng + lưu DB.
     - "Chỉ lưu giao dịch": Lưu DB mà không mở app ngân hàng (dùng khi trả tiền mặt hoặc chuyển qua máy khác).

---

## 4. Business Logic & Edge Cases

### 4.1. Giải mã chuẩn VietQR (EMVCo TLV Parser)
- Format EMVCo là chuỗi Tag (2 ký tự) + Length (2 ký tự) + Value:
  - `Tag 38`: Dữ liệu tài khoản Napas. Trong đó Subtag `00` là GUID `A000000727`, Subtag `01` là thông tin BIN ngân hàng (6 ký tự) + Số tài khoản.
  - `Tag 54`: Số tiền giao dịch (nếu là dynamic QR).
  - `Tag 62`: Dữ liệu bổ sung, Subtag `08` là nội dung giao dịch (Purpose of Transaction).
- Nếu mã QR không đúng chuẩn VietQR: Thông báo lỗi rõ ràng hoặc cho phép người dùng nhập tay.

### 4.2. Khởi chạy App Ngân hàng (Handoff v2)
- Tạo URI Napas VietQR chuẩn:
  `vietqr://transfer?bank={bankBin}&account={accountNumber}&amount={amount}&memo={encodedMemo}`
- Thứ tự ưu tiên khi người dùng đã chọn app cụ thể (`targetPackageName`):
  1. Thử `Intent(ACTION_VIEW, vietqrUri).setPackage(targetPackage)` — nếu app có đăng ký
     scheme Napas thì mở thẳng màn hình CK kèm thông tin (kết quả tốt nhất).
  2. Nếu `resolveActivity == null`: mở app trắng qua `getLaunchIntentForPackage`
     + Toast hướng dẫn dán thủ công (STK, số tiền, nội dung đã copy vào Clipboard).
- Không chọn app cụ thể: thử deep link chung, rồi fallback mở app của ngân hàng
  nhận (tra theo BIN) — cũng thử deep link gắn package trước, mở trắng sau.
- Ghi chú trung thực cho người dùng: hầu hết app ngân hàng VN không hỗ trợ điền
  sẵn form qua intent, nên Clipboard + hướng dẫn dán là luồng chính, deep link
  chỉ là best-effort.

### 4.4. Chuyển nhanh qua ảnh QR lưu Thư viện (luồng chính, universal)
- Vấn đề: deep link `vietqr://` tùy app (VD ZaloPay xử lý được nhờ hợp tác riêng
  + ký số với bank, MB Bank không). Không có API public nào để nhét STK/số tiền
  vào app bank. Hiện QR trên màn hình cũng vô dụng với 1 điện thoại (camera
  không quét được màn hình của chính nó).
- Giải pháp dùng tài liệu chính thức của bank: VCB Digibank và Techcombank Mobile
  đều hỗ trợ "quét QR từ thư viện ảnh" ngay trong màn hình quét. Luồng:
  1. App dựng mã VietQR ĐỘNG đủ số tiền + nội dung (`VietQrBuilder`), render
     bằng ZXing (`QrBitmapRenderer`, offline).
  2. Lưu ảnh PNG vào Thư viện (`QrImageSaver`: MediaStore `Pictures/YourHistory`
     từ Android 10 không cần quyền; Android 8-9 xin `WRITE_EXTERNAL_STORAGE`).
  3. Lưu lịch sử giao dịch + copy clipboard dự phòng, mở màn hình hướng dẫn.
  4. Người dùng chọn app bank của mình (tài khoản chuyển đi), bấm mở,
     trong app bank chọn Quét QR → ảnh từ Thư viện → STK/tiền/nội dung tự điền.
- `VietQrBuilder` (thuần Kotlin, `domain/parser`):
  - Dựng EMVCo: `00=01`, `01=12/11` (động/tĩnh), `38={00:A000000727, 01:{00:BIN, 01:STK}, 02:QRIBFTTA}`,
    `52=0000`, `53=704`, `54=số tiền`, `58=VN`, `59=tên`, `60=VN`, `62={08:nội dung}`,
    `63=CRC16-CCITT-FALSE` (poly `0x1021`, init `0xFFFF`).
  - Tên/nội dung chuẩn hóa ASCII in hoa, bỏ dấu tiếng Việt (tránh lỗi độ dài TLV).
- Nút ở `TransactionForm`: 1) Chuyển nhanh & Lưu (chính: chọn bank gửi → lưu ảnh
  + lịch sử → màn hình mở bank), 2) Mở app bank dán thủ công (fallback),
  3) Chỉ lưu.

### 4.3. Nhập số tiền & Nội dung chuyển khoản
- Ô số tiền lưu trữ chữ số thô (`amountText` digits-only), hiển thị nhóm 3 số
  cách nhau bằng dấu cách: `1000000` -> `1 000 000` (`formatAmountInput`, thuần
  Kotlin, unit-test được). `supportingText` hiển thị thêm dạng `formatCurrency`.
- Nội dung CK điền nhanh bằng tag: 8 tag mặc định (Cơm trưa, Cà phê, Xăng xe,
  Đi chợ, Tiền nhà, Trả nợ, Ăn vặt, Mua sắm). Chạm tag: trống thì gán, có rồi
  thì nối thêm (không trùng lặp). Người dùng tự thêm tag mới, lưu bền vào
  DataStore (`memo_tags`, phân cách `|||`) để lần sau dùng tiếp.

---

## 5. Kế hoạch Kiểm thử & Xác minh
- **Unit Test**: Test parser VietQR với chuỗi mẫu của các ngân hàng phổ biến (MB, VCB, ACB).
  Thêm test `formatAmountInput`: "", "5", "20000"->"20 000", "1000000"->"1 000 000".
  Thêm test `VietQrBuilderTest`: vector CRC `123456789`->`0x29B1`, round-trip
  build->parse, QR tĩnh không có tag 54.
- **Manual Test**: Kiểm tra camera quét nhạy, phân tích ảnh từ gallery, sao chép clipboard chính xác và mở đúng app ngân hàng.
  Kiểm tra handoff trên 2-3 app bank thật: trường hợp deep link được (mở kèm info)
  và trường hợp mở trắng + dán clipboard.
