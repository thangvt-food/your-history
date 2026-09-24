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

### 4.2. Khởi chạy App Ngân hàng (Handoff)
- Tạo URI Napas VietQR chuẩn:
  `vietqr://transfer?bank={bankBin}&account={accountNumber}&amount={amount}&memo={encodedMemo}`
- Thử mở qua `Intent(Intent.ACTION_VIEW, uri)`.
- Nếu thiết bị chưa đăng ký handler cho scheme này:
  - Copy toàn bộ thông tin quan trọng vào Clipboard (Số tài khoản, Số tiền, Nội dung).
  - Hiển thị BottomSheet danh sách các App Ngân hàng đang có sẵn trên máy (VCB Digibank, MB Bank, Techcombank Mobile, TPBank, BIDV SmartBanking, VPBank NEO, ACB ONE, MoMo, v.v.) kèm icon để người dùng bấm mở 1 chạm.

---

## 5. Kế hoạch Kiểm thử & Xác minh
- **Unit Test**: Test parser VietQR với chuỗi mẫu của các ngân hàng phổ biến (MB, VCB, ACB).
- **Manual Test**: Kiểm tra camera quét nhạy, phân tích ảnh từ gallery, sao chép clipboard chính xác và mở đúng app ngân hàng.
