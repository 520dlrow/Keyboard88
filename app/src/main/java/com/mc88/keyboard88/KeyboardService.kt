package com.mc88.keyboard88

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardService : InputMethodService() {

    private var currentLang = "EN"
    private var currentSize = 60
    private var isShift = false
    private lateinit var row1: LinearLayout
    private lateinit var row2: LinearLayout
    private lateinit var row3: LinearLayout
    private lateinit var btnLang: Button

    private val layouts = mapOf(
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
            listOf("Shift", "ظ", "ط", "ذ", "د", "ز", "ر", "و", "⌫")
        )
    )

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        row1 = view.findViewById(R.id.row1)
        row2 = view.findViewById(R.id.row2)
        row3 = view.findViewById(R.id.row3)
        btnLang = view.findViewById(R.id.btnLang)

        btnLang.setOnClickListener {
            currentLang = when (currentLang) {
                "EN" -> "FR"
                "FR" -> "AR"
                else -> "EN"
            }
            btnLang.text = "🌐 $currentLang"
            renderKeyboard()
        }

        view.findViewById<Button>(R.id.btnSize).setOnClickListener {
            currentSize = when (currentSize) {
                60 -> 75
                75 -> 90
                else -> 60
            }
            renderKeyboard()
        }

        view.findViewById<Button>(R.id.btnSpace).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }

        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
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

        view.findViewById<Button>(R.id.btnEnter).setOnClickListener {
            val ic = currentInputConnection
            if (ic != null) {
                val action = currentInputEditorInfo?.imeOptions and EditorInfo.IME_MASK_ACTION
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

        val currentLayout = layouts[currentLang] ?: layouts["EN"]!!

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
                params.setMargins(4, 4, 4, 4)
                btn.layoutParams = params
                btn.setBackgroundResource(R.drawable.btn_bg)
                btn.setTextColor(android.graphics.Color.WHITE)
                btn.textSize = 18f

                if (key == "Shift" || key == "⌫") {
                    btn.text = key
                    btn.textSize = 14f
                    if (key == "Shift") {
                        btn.setOnClickListener {
                            isShift = !isShift
                            renderKeyboard()
                        }
                    }
                } else {
                    btn.text = if (isShift) key.uppercase() else key.lowercase()
                    btn.setOnClickListener {
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
