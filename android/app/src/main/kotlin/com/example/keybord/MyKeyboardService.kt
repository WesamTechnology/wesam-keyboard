package com.example.keybord

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import android.content.pm.PackageManager
import android.Manifest
import android.os.Handler
import android.os.Looper

class MyKeyboardService : InputMethodService() {

    private var isArabic = true
    private var isSymbols = false
    private var isTajweed = false
    private var isCaps = false // متغير لتتبع حالة الأحرف الكبيرة

    // ✅ متغير لتخزين العرض الرئيسي للكيبورد للوصول إليه لاحقًا
    private var mainKeyboardView: LinearLayout? = null
    
    // ✅ زر التسجيل لتغيير شكله لاحقًا
    private var recordButton: Button? = null // New property
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechIntent: Intent? = null

    override fun onCreate() {
        super.onCreate()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA") // ضبط اللغة للعربية
        }
        setRecognitionListener()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    private fun setRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(applicationContext, "تحدث الآن... 🎙️", Toast.LENGTH_SHORT).show()
                updateRecordButtonState(true) // Start Animation
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                updateRecordButtonState(false) // End Animation
            }
            override fun onError(error: Int) {
                 val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الكلام"
                    SpeechRecognizer.ERROR_NETWORK -> "خطأ في الشبكة"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يرجى منح صلاحية الميكروفون"
                    else -> "خطأ في التسجيل: $error"
                }
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                updateRecordButtonState(false) // Reset on Error
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    typeTextWordByWord(text)
                }
                updateRecordButtonState(false) // Ensure reset
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    // كتابة النص كلمة كلمة (نفس فكرة النسخ)
    private fun typeTextWordByWord(text: String) {
        val inputConnection = currentInputConnection ?: return
        val words = text.split(" ")
        Thread {
            for (word in words) {
                // نستخدم Handler للتأكد من الكتابة على الـ Main Thread إذا لزم الأمر، 
                // ولكن inputConnection عادة يعمل من أي Thread. لتجنب المشاكل سنبقيها بسيطة.
                // لكن لتحديث الـ UI (لو أردنا) نحتاج Main Thread. هنا فقط نرسل نص.
                inputConnection.commitText("$word ", 1)
                Thread.sleep(150) // سرعة الكتابة
            }
        }.start()
    }
    private val arabicKeys = arrayOf(
        arrayOf("د", "ج", "ح", "خ", "ه", "ع", "غ", "ف", "ق", "ث", "ص", "ض"),
        arrayOf("ط", "ك", "م", "ن", "ت", "ا", "ل", "ب", "ي", "س", "ش"),
        arrayOf("⌫", "ظ", "ز", "و", "ة", "ى", "لا", "ر", "ؤ", "ئ", "ء", "ذ")
    )

    private val englishKeysUpper = arrayOf(
        arrayOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        arrayOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        arrayOf("⬆", "Z", "X", "C", "V", "B", "N", "M", "⌫")
    )

    private val englishKeysLower = arrayOf(
        arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        arrayOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        arrayOf("⬆", "z", "x", "c", "v", "b", "n", "m", "⌫")
    )

    private val symbolsKeys: Array<Array<String>> = arrayOf(
        arrayOf("!", "@", "#", "\$", "%", "^", "&", "*", "(", ")"),
        arrayOf("-", "_", "=", "+", "[", "]", "{", "}", "/", "\\"),
        arrayOf(".", ",", "؟", "!", ":", ";", "\"", "'", "…", "°", "⌫"),
        arrayOf("﷼", "€", "£", "¥", "<", ">", "|", "™", "©", "®")
    )

    private val tajweedKeys: Array<Array<String>> = arrayOf(
        arrayOf("َ", "ً", "ُ", "ٌ", "ِ", "ٍ", "ّ", "ْ", "ٓ"),
        arrayOf("ٰ", "ٖ", "ٗ", "ٔ", "ٕ", "ۡ", "ـ", "ࣰ", "ࣱ", "ࣲ"),
        arrayOf("⌫", "ۘ", "ۚ", "ۛ", "ۖ", "ۗ", "ۙ", "࣢"),
        arrayOf("۩", "۝", "۞", "﴾", "﴿", "ﷺ", "ﷲ", "﷽")
    )


    private fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        return prefs.getBoolean("flutter.isActivated", prefs.getBoolean("isActivated", false))
    }
    
    private fun updateRecordButtonState(isRecording: Boolean) {
        val btn = recordButton ?: return
        if (isRecording) {
            btn.text = "🔴" // رمز التسجيل
            btn.setBackgroundColor(0xFFFF0000.toInt()) // لون أحمر
            // تحديث المستمع للون الأحمر عند الضغط (اختياري، لكن جيد للتناسق)
            btn.setOnTouchListener(getOnTouchListener(btn, 0xFFFF0000.toInt(), 0xFFCC0000.toInt()))
        } else {
            btn.text = "🎙️" // الرمز الأصلي
            btn.setBackgroundColor(0xFF4A6FA5.toInt()) // العودة للأزرق
            btn.setOnTouchListener(getOnTouchListener(btn, 0xFF4A6FA5.toInt(), 0xFF3A5F95.toInt()))
        }
    }

    private fun startVoiceRecognition() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                speechRecognizer?.startListening(speechIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                updateRecordButtonState(false) // Reset on error
            }
        } else {
            Toast.makeText(this, "يرجى منح صلاحية الميكروفون للتطبيق", Toast.LENGTH_LONG).show()
            updateRecordButtonState(false) // Reset
            // لا يمكن طلب الصلاحيات runtime من داخل خدمة الكيبورد بسهولة في أندرويد الحديث،
            // يجب على المستخدم تفعيلها من الإعدادات.
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onCreateInputView(): View {
        if (!isActivated(this)) return createLockedView()
        isCaps = false
        mainKeyboardView = createMainKeyboardView()
        return mainKeyboardView!!
    }

    private fun createLockedView(): View {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(0xFFF6F8FA.toInt())
        layout.setPadding(40, 40, 40, 40)

        val lockIcon = ImageView(this)
        lockIcon.setImageResource(android.R.drawable.ic_lock_lock)
        lockIcon.setColorFilter(0xFF4A6FA5.toInt())
        layout.addView(lockIcon)

        val txt = TextView(this)
        txt.text = "لوحة المفاتيح مقفلة 🔒\nيرجى تفعيلها من التطبيق أولاً"
        txt.textSize = 18f
        txt.gravity = Gravity.CENTER
        txt.setTextColor(0xFF000000.toInt())
        txt.setPadding(0, 20, 0, 20)
        layout.addView(txt)

        val openApp = Button(this)
        openApp.text = "فتح التطبيق للتفعيل"
        openApp.setBackgroundColor(0xFF4A6FA5.toInt())
        openApp.setTextColor(0xFFFFFFFF.toInt())
        openApp.setPadding(16, 12, 16, 12)
        openApp.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "تعذر فتح التطبيق", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(openApp)
        return layout
    }

    private fun createMainKeyboardView(): LinearLayout {
        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setBackgroundColor(0xFFF6F8FA.toInt())

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.45).toInt()
        )
        mainLayout.layoutParams = params

        val topBar = LinearLayout(this)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.setPadding(8, 8, 8, 8)
        topBar.setBackgroundColor(0xFFDCE3EC.toInt())

        val pasteButton = Button(this)
        pasteButton.text = "📋 لصق"
        pasteButton.setBackgroundColor(0xFF4A6FA5.toInt())
        pasteButton.setTextColor(0xFFFFFFFF.toInt())
        pasteButton.textSize = 16f
        pasteButton.setOnTouchListener(getOnTouchListener(pasteButton, 0xFF4A6FA5.toInt(), 0xFF3A5F95.toInt()))
        pasteButton.setOnClickListener { pasteWordByWord() }

        val copyButton = Button(this)
        copyButton.text = "📄 نسخ"
        copyButton.setBackgroundColor(0xFF4A6FA5.toInt())
        copyButton.setTextColor(0xFFFFFFFF.toInt())
        copyButton.textSize = 16f
        copyButton.setOnTouchListener(getOnTouchListener(copyButton, 0xFF4A6FA5.toInt(), 0xFF3A5F95.toInt()))
        copyButton.setOnClickListener { copySelectedText() }

        val langButton = Button(this)
        langButton.text = if (isArabic) "🌐 EN" else "🌐 AR"
        langButton.setBackgroundColor(0xFF4A6FA5.toInt())
        langButton.setTextColor(0xFFFFFFFF.toInt())
        langButton.textSize = 16f
        langButton.setOnTouchListener(getOnTouchListener(langButton, 0xFF4A6FA5.toInt(), 0xFF3A5F95.toInt()))
        langButton.setOnClickListener {
            isArabic = !isArabic
            isSymbols = false
            isTajweed = false
            isCaps = false
            langButton.text = if (isArabic) "🌐 EN" else "🌐 AR"
            updateKeyboard(mainLayout)
        }

        val symbolButton = Button(this)
        symbolButton.text = "🔣"
        symbolButton.setBackgroundColor(0xFF4A6FA5.toInt())
        symbolButton.setTextColor(0xFFFFFFFF.toInt())
        symbolButton.textSize = 16f
        symbolButton.setOnTouchListener(getOnTouchListener(symbolButton, 0xFF4A6FA5.toInt(), 0xFF3A5F95.toInt()))
        symbolButton.setOnClickListener {
            isSymbols = !isSymbols
            isTajweed = false
            updateKeyboard(mainLayout)
        }

        val tajweedButton = Button(this)
        tajweedButton.text = "\uD83D\uDD4B"
        tajweedButton.setBackgroundColor(0xFF4A6FA5.toInt())
        tajweedButton.setTextColor(0xFFFFFFFF.toInt())
        tajweedButton.textSize = 16f
        tajweedButton.setOnTouchListener(getOnTouchListener(tajweedButton, 0xFF4A6FA5.toInt(), 0xFF3A5F95.toInt()))
        tajweedButton.setOnClickListener {
            isTajweed = !isTajweed
            isSymbols = false
            updateKeyboard(mainLayout)
        }
        
        val _recordButton = Button(this) // Use temporary variable
        _recordButton.text = "🎙️"
        _recordButton.setBackgroundColor(0xFF4A6FA5.toInt())
        _recordButton.setTextColor(0xFFFFFFFF.toInt())
        _recordButton.textSize = 16f
        _recordButton.setOnTouchListener(getOnTouchListener(_recordButton, 0xFF4A6FA5.toInt(), 0xFF3A5F95.toInt()))
        _recordButton.setOnClickListener { startVoiceRecognition() }
        
        recordButton = _recordButton // Assign to property

        topBar.addView(pasteButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topBar.addView(copyButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topBar.addView(langButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f))
        topBar.addView(_recordButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f))
        topBar.addView(symbolButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f))
        topBar.addView(tajweedButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f))
        mainLayout.addView(topBar)

        val keysLayout = createKeysLayout()
        val keysParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        keysLayout.layoutParams = keysParams
        mainLayout.addView(keysLayout)

        return mainLayout
    }

    private fun createKeysLayout(): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.tag = "keysLayout"

        if (!isSymbols && !isTajweed) {
            val numbersRow = LinearLayout(this)
            numbersRow.orientation = LinearLayout.HORIZONTAL
            numbersRow.gravity = Gravity.CENTER
            numbersRow.layoutDirection = if (isArabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            val numbers = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            for (num in numbers) numbersRow.addView(makeKeyButton(num))
            layout.addView(numbersRow)
        }

        val keys = when {
            isTajweed -> tajweedKeys
            isSymbols -> symbolsKeys
            isArabic -> arabicKeys
            isCaps -> englishKeysUpper
            else -> englishKeysLower
        }

        for (row in keys) {
            val rowLayout = LinearLayout(this)
            rowLayout.orientation = LinearLayout.HORIZONTAL
            rowLayout.gravity = Gravity.CENTER
            rowLayout.layoutDirection = if (isArabic || isTajweed) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            for (key in row) {
                when (key) {
                    "⌫" -> rowLayout.addView(createDeleteButton())
                    "⬆" -> rowLayout.addView(createShiftButton())
                    else -> rowLayout.addView(makeKeyButton(key))
                }
            }
            layout.addView(rowLayout)
        }

        val bottomRow = LinearLayout(this)
        bottomRow.orientation = LinearLayout.HORIZONTAL
        bottomRow.gravity = Gravity.CENTER

        val enterButton = Button(this)
        enterButton.text = "⏎"
        enterButton.setBackgroundColor(0xFFFFFFFF.toInt())
        enterButton.textSize = 18f
        enterButton.setOnTouchListener(getOnTouchListener(enterButton, 0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt()))
        enterButton.setOnClickListener {
            currentInputConnection.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
        }

        val space = Button(this)
        space.text = when {
            isTajweed -> " "
            isSymbols -> "—"
            else -> "مسافة"
        }
        space.setBackgroundColor(0xFFFFFFFF.toInt())
        space.textSize = 18f
        space.setOnTouchListener(getOnTouchListener(space, 0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt()))
        space.setOnClickListener { currentInputConnection.commitText(" ", 1) }

        bottomRow.addView(enterButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f))
        bottomRow.addView(space, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        layout.addView(bottomRow)

        return layout
    }

    private fun createDeleteButton(): Button {
        val delete = Button(this)
        delete.text = "⌫"
        delete.setBackgroundColor(0xFFFFFFFF.toInt())
        delete.textSize = 18f

        val deleteRunnable = object : Runnable {
            override fun run() {
                val ic = currentInputConnection
                val selectedText = ic?.getSelectedText(0)
                if (!selectedText.isNullOrEmpty()) {
                    ic.commitText("", 1)
                } else {
                    ic?.deleteSurroundingText(1, 0)
                }
                delete.postDelayed(this, 80)
            }
        }

        val normalColor = 0xFFFFFFFF.toInt()
        val pressedColor = 0xFFE0E0E0.toInt()
        
        delete.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start()
                    v.setBackgroundColor(pressedColor)
                    v.isPressed = true
                    v.post(deleteRunnable)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                    v.setBackgroundColor(normalColor)
                    v.isPressed = false
                    v.removeCallbacks(deleteRunnable)
                }
            }
            true
        }

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        params.setMargins(4, 4, 4, 4)
        delete.layoutParams = params
        return delete
    }

    private fun createShiftButton(): Button {
        val shiftButton = Button(this)
        shiftButton.text = "⬆"
        shiftButton.textSize = 18f

        val normalColor: Int
        val pressedColor: Int

        if (isCaps) {
            normalColor = 0xFF4A6FA5.toInt()
            pressedColor = 0xFF3A5F95.toInt()
            shiftButton.setTextColor(Color.WHITE)
        } else {
            normalColor = 0xFFFFFFFF.toInt()
            pressedColor = 0xFFE0E0E0.toInt()
            shiftButton.setTextColor(Color.BLACK)
        }
        shiftButton.setBackgroundColor(normalColor)
        
        shiftButton.setOnTouchListener(getOnTouchListener(shiftButton, normalColor, pressedColor))

        shiftButton.setOnClickListener {
            isCaps = !isCaps
            // ✅ تم التصحيح هنا: استدعاء دالة التحديث باستخدام المتغير الذي يحمل العرض الرئيسي
            mainKeyboardView?.let { updateKeyboard(it) }
        }

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        params.setMargins(4, 4, 4, 4)
        shiftButton.layoutParams = params
        return shiftButton
    }

    private fun getOnTouchListener(view: View, normalColor: Int, pressedColor: Int): View.OnTouchListener {
        return View.OnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start()
                    v.setBackgroundColor(pressedColor)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                    v.setBackgroundColor(normalColor)
                }
            }
            false // Return false to let OnClickListener handle the click
        }
    }

    private fun makeKeyButton(key: String): Button {
        val button = Button(this)
        button.text = key
        button.textSize = 20f
        button.setTextColor(0xFF1B1B1B.toInt())
        val normalColor = 0xFFFFFFFF.toInt()
        val pressedColor = 0xFFE0E0E0.toInt()
        button.setBackgroundColor(normalColor)
        button.setPadding(8, 16, 8, 16)
        button.isAllCaps = false
        
        // Apply scale animation
        button.setOnTouchListener(getOnTouchListener(button, normalColor, pressedColor))

        if (key == "ا") {
            button.setOnLongClickListener {
                showCharVariantsPopup(button, arrayOf("أ", "إ", "آ"))
                true
            }
        }

        button.setOnClickListener { onKeyPress(key) }

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        params.setMargins(4, 4, 4, 4)
        button.layoutParams = params
        return button
    }

    private fun showCharVariantsPopup(anchorView: View, variants: Array<String>) {
        val popupView = LinearLayout(this)
        popupView.orientation = LinearLayout.HORIZONTAL
        popupView.setBackgroundColor(0xFFEEEEEE.toInt())
        popupView.setPadding(10, 10, 10, 10)

        val popupWindow = PopupWindow(popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)

        popupWindow.isFocusable = false
        popupWindow.isTouchable = true

        val ic = currentInputConnection

        for (variant in variants) {
            val button = Button(this)
            button.text = variant
            button.textSize = 20f
            button.setOnClickListener {
                ic?.commitText(variant, 1)
                popupWindow.dismiss()
            }
            popupView.addView(button)
        }

        popupWindow.showAsDropDown(anchorView, 0, -anchorView.height * 2)
    }

    private fun updateKeyboard(mainLayout: LinearLayout) {
        val oldKeysLayout = mainLayout.findViewWithTag<LinearLayout>("keysLayout")
        mainLayout.removeView(oldKeysLayout)
        val newKeysLayout = createKeysLayout()
        mainLayout.addView(newKeysLayout)
    }

    private fun onKeyPress(key: String) {
        val ic = currentInputConnection ?: return

        val nonCombiningSymbols = arrayOf("۩", "۝", "۞", "﴾", "﴿", "ﷺ", "ﷲ", "﷽")
        val isCombiningMark = tajweedKeys.any { row -> row.contains(key) } && !nonCombiningSymbols.contains(key)

        if (isCombiningMark) {
            ic.commitText(key, 0)
        } else {
            ic.commitText(key, 1)
        }
    }

    private fun pasteWordByWord() {
        val inputConnection = currentInputConnection
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip

        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).coerceToText(this).toString()
            val words = text.split(" ")
            Thread {
                for (word in words) {
                    inputConnection?.commitText("$word ", 1)
                    Thread.sleep(250)
                }
            }.start()
        } else {
            Toast.makeText(this, "لا يوجد نص للّصق", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copySelectedText() {
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("copiedText", selectedText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "تم النسخ ✅", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "لا يوجد نص محدد للنسخ", Toast.LENGTH_SHORT).show()
        }
    }
}
