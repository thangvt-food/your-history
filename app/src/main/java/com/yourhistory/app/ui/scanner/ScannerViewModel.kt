package com.yourhistory.app.ui.scanner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.yourhistory.app.domain.model.VietQrData
import com.yourhistory.app.domain.parser.VietQrParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScannerEvent {
    data class QrParsedSuccessfully(val data: VietQrData) : ScannerEvent()
    data class QrParseFailed(val message: String) : ScannerEvent()
}

class ScannerViewModel : ViewModel() {

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _events = MutableSharedFlow<ScannerEvent>()
    val events: SharedFlow<ScannerEvent> = _events.asSharedFlow()

    private var hasScanned = false

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    fun onBarcodeDetected(rawString: String) {
        if (hasScanned) return
        val parsed = VietQrParser.parse(rawString)
        if (parsed != null) {
            hasScanned = true
            viewModelScope.launch {
                _events.emit(ScannerEvent.QrParsedSuccessfully(parsed))
            }
        }
    }

    fun processGalleryImage(context: Context, imageUri: Uri) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val rawValue = barcodes.firstOrNull()?.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        val parsed = VietQrParser.parse(rawValue)
                        if (parsed != null) {
                            viewModelScope.launch {
                                _events.emit(ScannerEvent.QrParsedSuccessfully(parsed))
                            }
                        } else {
                            viewModelScope.launch {
                                _events.emit(ScannerEvent.QrParseFailed("Mã QR không đúng định dạng VietQR hoặc chưa được hỗ trợ."))
                            }
                        }
                    } else {
                        viewModelScope.launch {
                            _events.emit(ScannerEvent.QrParseFailed("Không tìm thấy mã QR trong bức ảnh này."))
                        }
                    }
                }
                .addOnFailureListener {
                    viewModelScope.launch {
                        _events.emit(ScannerEvent.QrParseFailed("Lỗi khi đọc ảnh: ${it.localizedMessage}"))
                    }
                }
        } catch (e: Exception) {
            viewModelScope.launch {
                _events.emit(ScannerEvent.QrParseFailed("Không thể tải ảnh: ${e.localizedMessage}"))
            }
        }
    }

    fun resetScanner() {
        hasScanned = false
    }
}
