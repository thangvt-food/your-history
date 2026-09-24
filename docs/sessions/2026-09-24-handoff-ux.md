# Session Log: 2026-09-24 - Handoff v2 & UX Nhập tiền/Tag

- **Vấn đề người dùng báo**: chọn app ngân hàng chỉ mở trắng, không có thông tin
  CK (chỉ nằm trong clipboard) — chậm hơn dùng app bank trực tiếp. Ô nhập tiền
  khó nhìn số 0. Nội dung CK muốn điền nhanh bằng tag + tự thêm tag.
- **Tài liệu đặc tả**: [`docs/specs/vietqr-expense-tracking.md`](../specs/vietqr-expense-tracking.md) (§4.2 v2, §4.3)

---

## 1. Thay đổi

### Handoff (`BankingHandoffManager.kt`)
- Khi có `targetPackageName`: thử `Intent(ACTION_VIEW, vietqrUri).setPackage(pkg)`
  trước, `resolveActivity != null` mới mở + Toast "Đã mở app kèm thông tin".
- Fallback: `getLaunchIntentForPackage` + Toast LONG hướng dẫn dán thủ công.
- Không chọn app: deep link chung -> deep link gắn package bank (tra BIN) ->
  mở trắng + Toast dán.
- BottomSheet (`TransactionFormScreen.kt`) đổi nội dung trung thực: deep link chỉ là
  best-effort, luồng chính là dán từ Clipboard.

### Nhập tiền (`TransactionFormViewModel.kt`, `TransactionFormScreen.kt`)
- `amountText` digits-only (`trimStart('0')`), hiển thị
  `formatAmountInput` nhóm 3 số bằng dấu cách ("1000000" -> "1 000 000").
- `supportingText` hiển thị thêm `formatCurrency` ("1.000.000 đ").
- `formatAmountInput` thuần Kotlin, đặt trong `companion object` để unit test JVM.

### Tag memo (`UserPreferencesRepository.kt`, `TransactionFormViewModel.kt`, UI)
- 8 tag mặc định: Cơm trưa, Cà phê, Xăng xe, Đi chợ, Tiền nhà, Trả nợ, Ăn vặt, Mua sắm.
- Chạm tag: trống -> gán, có rồi -> nối thêm (case-insensitive, không trùng).
- Tự thêm tag: input + nút "Thêm", lưu DataStore key `memo_tags` (join `|||`).

### DI (`AppModule.kt`)
- `TransactionFormViewModel(get(), get())` (thêm `UserPreferencesRepository`).

## 2. Kiểm thử
- Chưa chạy `./gradlew assembleDebug` local (máy không có Android SDK) — cần CI xác minh.
- Cần manual test trên máy thật với 2-3 app bank: deep link được vs mở trắng + dán.
- Nên bổ sung unit test `formatAmountInput` trong CI sau.
