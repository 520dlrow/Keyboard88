package com.mc88.keyboard88

import android.inputmethodservice.InputMethodService
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout

class KeyboardService : InputMethodService() {

    private var currentLang = "EN"
    private var currentSize = 65
    private var isShift = false
    private var isSymbols = false
    
    private lateinit var row1: LinearLayout
    private lateinit var row2: LinearLayout
    private lateinit var row3: LinearLayout
    private lateinit var btnLang: Button
    private lateinit var btn123: Button

    private val textLayouts = mapOf(
        "EN" to listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Shift", "Z", "X", "C", "V", "B", "N", "M", "⌫")
        ),
        "FR" to listOf(
            listOf("A", "Z", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("Q", "S", "D", "F", "G", "H", "J", "K", "L", "M"),
            listOf("Shift", "W", "X", "C", "V", "B", "N", "⌫")
        ),
        "AR" to listOf(
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح"),
            listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك"),
            listOf("Shift", "ظ", "ط", "ذ", "د", "ز", "ر", "و", "ة", "ء", "⌫")
        )
    )

    private val symbolLayouts = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")"),
        listOf("Shift", "\"", "'", ":", ";", "!", "?", "⌫")
    )

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        row1 = view.findViewById(R.id.row1)
        row2 = view.findViewById(R.id.row2)
        row3 = view.findViewById(R.id.row3)
        btnLang = view.findViewById(R.id.btnLang)
        btn123 = view.findViewById(R.id.btn123)

        btnLang.setOnClickListener {
            performHaptic(view)
            if (isSymbols) isSymbols = false
            currentLang = when (currentLang) {
                "EN" -> "FR"
                "FR" -> "AR"
                else -> "EN"
            }
            btnLang.text = "🌐 $currentLang"
            renderKeyboard()
        }

        btn123.setOnClickListener {
            performHaptic(view)
            isSymbols = !isSymbols
            if (isSymbols) {
                btn123.text = "ABC"
            } else {
                btn123.text = "123"
            }
            renderKeyboard()
        }

        view.findViewById<Button>(R.id.btnSize).setOnClickListener {
            performHaptic(view)
            currentSize = when (currentSize) {
                65 -> 80
                80 -> 95
                else -> 65
            }
            renderKeyboard()
        }

        view.findViewById<Button>(R.id.btnSpace).setOnClickListener {
            performHaptic(view)
            currentInputConnection?.commitText(" ", 1)
        }

        view.findViewById<Button>(R.id.btnEnter).setOnClickListener {
            performHaptic(view)
            val ic = currentInputConnection
            if (ic != null) {
                val imeOptions = currentInputEditorInfo?.imeOptions ?: 0
                val action = imeOptions and EditorInfo.IME_MASK_ACTION
                if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.commitText("\n", 1)
                } else {
                    ic.performEditorAction(action)
                }
            }
        }

        renderKeyboard()
        return view
    }

    private fun renderKeyboard() {
        row1.removeAllViews()
        row2.removeAllViews()
        row3.removeAllViews()

        val currentLayout = if (isSymbols) symbolLayouts else (textLayouts[currentLang] ?: textLayouts["EN"]!!)

        for (rowIndex in 0..2) {
            val rowLayout = when (rowIndex) {
                0 -> row1
                1 -> row2
                else -> row3
            }
            rowLayout.layoutParams.height = dpToPx(currentSize)
            
            for (key in currentLayout[rowIndex]) {
                val btn = Button(this)
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                params.setMargins(3, 3, 3, 3)
                btn.layoutParams = params
                btn.setBackgroundResource(R.drawable.btn_bg)
                btn.setTextColor(android.graphics.Color.WHITE)
                btn.textSize = 18f

                if (key == "Shift" || key == "⌫") {
                    btn.text = key
                    btn.textSize = 14f
                    if (key == "Shift") {
                        btn.setOnClickListener {
                            performHaptic(btn)
                            isShift = !isShift
                            renderKeyboard()
                        }
                    } else {
                        btn.setOnClickListener {
                            performHaptic(btn)
                            val ic = currentInputConnection
                            if (ic != null) {
                                val selectedText = ic.getSelectedText(0)
                                if (selectedText.isNullOrEmpty()) {
                                    ic.deleteSurroundingText(1, 0)
                                } else {
                                    ic.commitText("", 1)
                                }
                            }
                        }
                    }
                } else {
                    btn.text = if (isShift) key.uppercase() else key.lowercase()
                    btn.setOnClickListener {
                        performHaptic(btn)
                        val text = if (isShift) key.uppercase() else key.lowercase()
                        currentInputConnection?.commitText(text, 1)
                        if (isShift) {
                            isShift = false
                            renderKeyboard()
                        }
                    }
                }
                rowLayout.addView(btn)
            }
        }
    }

    private fun performHaptic(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
