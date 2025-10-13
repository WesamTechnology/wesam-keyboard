package com.example.keybord

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.*

class MyKeyboardService : InputMethodService() {

    private var isArabic = true
    private var isSymbols = false

    private val arabicKeys = arrayOf(
        arrayOf("د", "ج", "ح", "خ", "ه", "ع", "غ", "ف", "ق", "ث", "ص", "ض"),
        arrayOf("ط", "ك", "م", "ن", "ت", "ا", "ل", "ب", "ي", "س", "ش"),
        arrayOf("⌫", "ظ", "ز", "و", "ة", "ى", "لا", "ر", "ؤ", "ئ", "ء", "ذ")
    )

    private val englishKeys = arrayOf(
        arrayOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        arrayOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        arrayOf("Z", "X", "C", "V", "B", "N", "M", "⌫")
    )

    private val symbolsKeys = arrayOf(
        arrayOf("!", "@", "#", "\$", "%", "^", "&", "*", "(", ")"),
        arrayOf("-", "_", "=", "+", "[", "]", "{", "}", "/", "\\"),
        arrayOf(".", ",", "؟", "!", ":", ";", "\"", "'", "…", "°", "⌫")
    )

    private fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        return prefs.getBoolean("flutter.isActivated", prefs.getBoolean("isActivated", false))
    }

    override fun onCreateInputView(): View {
        if (!isActivated(this)) return createLockedView()
        return createMainKeyboardView()
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

    private fun createMainKeyboardView(): View {
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
        pasteButton.setOnClickListener { pasteWordByWord() }

        val copyButton = Button(this)
        copyButton.text = "📄 نسخ"
        copyButton.setBackgroundColor(0xFF4A6FA5.toInt())
        copyButton.setTextColor(0xFFFFFFFF.toInt())
        copyButton.textSize = 16f
        copyButton.setOnClickListener { copySelectedText() }

        val langButton = Button(this)
        langButton.text = if (isArabic) "🌐 EN" else "🌐 AR"
        langButton.setBackgroundColor(0xFF4A6FA5.toInt())
        langButton.setTextColor(0xFFFFFFFF.toInt())
        langButton.textSize = 16f
        langButton.setOnClickListener {
            isArabic = !isArabic
            isSymbols = false
            langButton.text = if (isArabic) "🌐 EN" else "🌐 AR"
            updateKeyboard(mainLayout)
        }

        val symbolButton = Button(this)
        symbolButton.text = "🔣"
        symbolButton.setBackgroundColor(0xFF4A6FA5.toInt())
        symbolButton.setTextColor(0xFFFFFFFF.toInt())
        symbolButton.textSize = 16f
        symbolButton.setOnClickListener {
            isSymbols = !isSymbols
            updateKeyboard(mainLayout)
        }

        topBar.addView(pasteButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topBar.addView(copyButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topBar.addView(langButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f))
        topBar.addView(symbolButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f))
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

        if (!isSymbols) {
            val numbersRow = LinearLayout(this)
            numbersRow.orientation = LinearLayout.HORIZONTAL
            numbersRow.gravity = Gravity.CENTER
            numbersRow.layoutDirection = if (isArabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            val numbers = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            for (num in numbers) numbersRow.addView(makeKeyButton(num))
            layout.addView(numbersRow)
        }

        val keys = when {
            isSymbols -> symbolsKeys
            isArabic -> arabicKeys
            else -> englishKeys
        }

        for (row in keys) {
            val rowLayout = LinearLayout(this)
            rowLayout.orientation = LinearLayout.HORIZONTAL
            rowLayout.gravity = Gravity.CENTER
            rowLayout.layoutDirection = if (isArabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            for (key in row) {
                if (key == "⌫") {
                    rowLayout.addView(createDeleteButton())
                } else {
                    rowLayout.addView(makeKeyButton(key))
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
        enterButton.setOnClickListener {
            currentInputConnection.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
        }

        val space = Button(this)
        space.text = if (isSymbols) "—" else "مسافة"
        space.setBackgroundColor(0xFFFFFFFF.toInt())
        space.textSize = 18f
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

        delete.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    v.post(deleteRunnable)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
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

    private fun makeKeyButton(key: String): Button {
        val button = Button(this)
        button.text = key
        button.textSize = 20f
        button.setTextColor(0xFF1B1B1B.toInt())
        button.setBackgroundColor(0xFFFFFFFF.toInt())
        button.setPadding(8, 16, 8, 16)

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

    // ✅ تم تعديل هذه الدالة لمنع الكيبورد من الاختفاء
    private fun showCharVariantsPopup(anchorView: View, variants: Array<String>) {
        val popupView = LinearLayout(this)
        popupView.orientation = LinearLayout.HORIZONTAL
        popupView.setBackgroundColor(0xFFEEEEEE.toInt())
        popupView.setPadding(10, 10, 10, 10)

        val popupWindow = PopupWindow(popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)

        // ✅ السطر الأهم: هذا يمنع النافذة المنبثقة من سرقة التركيز
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
        currentInputConnection?.commitText(key, 1)
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
