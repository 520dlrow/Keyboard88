package com.mc88.keyboard88

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class KeyboardService : InputMethodService() {

    private var currentLang = "EN"
    private var isShift = false
    private var isSymbols = false
    
    private lateinit var rowNumbers: LinearLayout
    private lateinit var row1: LinearLayout
    private lateinit var row2: LinearLayout
    private lateinit var row3: LinearLayout
    private lateinit var btnLang: Button
    private lateinit var btnShift: Button
    private lateinit var pred1: TextView
    private lateinit var pred2: TextView
    private lateinit var pred3: TextView
    
    private var currentWord = ""

    private val dictionary = listOf("the", "and", "you", "that", "was", "for", "are", "with", "his", "they", "this", "have", "from", "one", "had", "word", "but", "not", "what", "all", "were", "when", "your", "can", "said", "there", "use", "an", "each", "which", "she", "do", "how", "their", "if", "will", "up", "other", "about", "out", "many", "then", "them", "these", "so", "some", "her", "would", "make", "like", "him", "into", "time", "has", "look", "two", "more", "write", "go", "see", "number", "no", "way", "could", "people", "my", "than", "first", "water", "been", "call", "who", "oil", "its", "now", "find", "long", "down", "day", "did", "get", "come", "made", "may", "part")

    private val layouts = mapOf(
        "EN" to listOf(
            listOf("Q+" to "+", "W=" to "=", "E%" to "%", "R/" to "/", "T_" to "_", "Y-" to "-", "U:" to ":", "I;" to ";", "O(" to "(", "P)" to ")"),
            listOf("A!" to "!", "S@" to "@", "D#" to "#", "F$" to "$", "G&" to "&", "H*" to "*", "J\"" to "\"", "K'" to "'", "L?" to "?"),
            listOf("⇧" to "", "Z" to "", "X" to "", "C" to "", "V" to "", "B" to "", "N" to "", "M" to "", "⌫" to "")
        ),
        "FR" to listOf(
            listOf("A+" to "+", "Z=" to "=", "E%" to "%", "R/" to "/", "T_" to "_", "Y-" to "-", "U:" to ":", "I;" to ";", "O(" to "(", "P)" to ")"),
            listOf("Q!" to "!", "S@" to "@", "D#" to "#", "F$" to "$", "G&" to "&", "H*" to "*", "J\"" to "\"", "K'" to "'", "L?" to "?", "M" to ""),
            listOf("⇧" to "", "W" to "", "X" to "", "C" to "", "V" to "", "B" to "", "N" to "", "⌫" to "")
        ),
        "AR" to listOf(
            listOf("ض" to "", "ص" to "", "ث" to "", "ق" to "", "ف" to "", "غ" to "", "ع" to "", "ه" to "", "خ" to "", "ح" to ""),
            listOf("ش" to "", "س" to "", "ي" to "", "ب" to "", "ل" to "", "ا" to "", "ت" to "", "ن" to "", "م" to "", "ك" to ""),
            listOf("⇧" to "", "ظ" to "", "ط" to "", "ذ" to "", "د" to "", "ز" to "", "ر" to "", "و" to "", "ة" to "", "ء" to "", "⌫" to "")
        )
    )

    private val symbolLayouts = listOf(
        listOf("1" to "", "2" to "", "3" to "", "4" to "", "5" to "", "6" to "", "7" to "", "8" to "", "9" to "", "0" to ""),
        listOf("-" to "", "/" to "", ":" to "", ";" to "", "(" to "", ")" to "", "$" to "", "&" to "", "@" to "", "\"" to ""),
        listOf("⇧" to "", "." to "", "," to "", "?" to "", "!" to "", "'" to "", "⌫" to "")
    )

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        rowNumbers = view.findViewById(R.id.rowNumbers)
        row1 = view.findViewById(R.id.row1)
        row2 = view.findViewById(R.id.row2)
        row3 = view.findViewById(R.id.row3)
        btnLang = view.findViewById(R.id.btnLang)
        btnShift = view.findViewById(R.id.btnShift)
        pred1 = view.findViewById(R.id.pred1)
        pred2 = view.findViewById(R.id.pred2)
        pred3 = view.findViewById(R.id.pred3)

        btnLang.setOnClickListener {
            haptic(it)
            currentLang = when (currentLang) {
                "EN" -> "FR"
                "FR" -> "AR"
                else -> "EN"
            }
            btnLang.text = currentLang
            renderKeyboard()
        }

        btnShift.setOnClickListener {
            haptic(it)
            isShift = !isShift
            renderKeyboard()
        }

        view.findViewById<Button>(R.id.btnSpace).setOnClickListener {
            haptic(it)
            currentInputConnection?.commitText(" ", 1)
            currentWord = ""
            updatePredictions()
        }

        view.findViewById<Button>(R.id.btnComma).setOnClickListener {
            haptic(it)
            currentInputConnection?.commitText(",", 1)
        }

        view.findViewById<Button>(R.id.btnDot).setOnClickListener {
            haptic(it)
            currentInputConnection?.commitText(".", 1)
        }

        view.findViewById<Button>(R.id.btnClipboard).setOnClickListener {
            haptic(it)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    currentInputConnection?.commitText(text, 1)
                    Toast.makeText(this, "Pasted!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Clipboard empty", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.btnEnter).setOnClickListener {
            haptic(it)
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

        pred1.setOnClickListener { selectPrediction(pred1.text.toString()) }
        pred2.setOnClickListener { selectPrediction(pred2.text.toString()) }
        pred3.setOnClickListener { selectPrediction(pred3.text.toString()) }

        renderKeyboard()
        return view
    }

    private fun renderKeyboard() {
        rowNumbers.removeAllViews()
        row1.removeAllViews()
        row2.removeAllViews()
        row3.removeAllViews()

        if (!isSymbols && (currentLang == "EN" || currentLang == "FR")) {
            for (i in 1..10) {
                val num = if (i == 10) "0" else i.toString()
                val btn = Button(this)
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                params.setMargins(2, 2, 2, 2)
                btn.layoutParams = params
                btn.setBackgroundResource(R.drawable.btn_touchwiz)
                btn.setTextColor(android.graphics.Color.WHITE)
                btn.text = num
                btn.textSize = 18f
                btn.setOnClickListener {
                    haptic(it)
                    currentInputConnection?.commitText(num, 1)
                }
                rowNumbers.addView(btn)
            }
        }

        val currentLayout = if (isSymbols) symbolLayouts else (layouts[currentLang] ?: layouts["EN"]!!)

        for (rowIndex in 0..2) {
            val rowLayout = when (rowIndex) {
                0 -> row1
                1 -> row2
                else -> row3
            }
            
            val rowList = currentLayout[rowIndex]
            for (item in rowList) {
                val key = item.first
                val secondary = item.second

                if (key == "⇧" || key == "⌫") {
                    val btn = Button(this)
                    val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.5f)
                    params.setMargins(2, 2, 2, 2)
                    btn.layoutParams = params
                    btn.setBackgroundResource(R.drawable.btn_special)
                    btn.setTextColor(android.graphics.Color.WHITE)
                    btn.text = key
                    btn.textSize = 18f
                    if (key == "⇧") {
                        btn.setOnClickListener {
                            haptic(btn)
                            isShift = !isShift
                            renderKeyboard()
                        }
                    } else {
                        btn.setOnClickListener {
                            haptic(btn)
                            val ic = currentInputConnection
                            if (ic != null) {
                                val selectedText = ic.getSelectedText(0)
                                if (selectedText.isNullOrEmpty()) {
                                    ic.deleteSurroundingText(1, 0)
                                    if (currentWord.isNotEmpty()) {
                                        currentWord = currentWord.dropLast(1)
                                        updatePredictions()
                                    }
                                } else {
                                    ic.commitText("", 1)
                                }
                            }
                        }
                    }
                    rowLayout.addView(btn)
                } else {
                    val keyView = LayoutInflater.from(this).inflate(R.layout.key_touchwiz, rowLayout, false)
                    val tvPrimary = keyView.findViewById<TextView>(R.id.tvPrimary)
                    val tvSecondary = keyView.findViewById<TextView>(R.id.tvSecondary)

                    val displayText = if (isShift) key.uppercase() else key.lowercase()
                    tvPrimary.text = displayText
                    tvSecondary.text = secondary

                    keyView.setOnClickListener {
                        haptic(it)
                        val textToCommit = if (isShift) key.uppercase() else key.lowercase()
                        currentInputConnection?.commitText(textToCommit, 1)
                        currentWord += textToCommit
                        updatePredictions()
                        if (isShift) {
                            isShift = false
                            renderKeyboard()
                        }
                    }

                    if (secondary.isNotEmpty()) {
                        keyView.setOnLongClickListener {
                            haptic(it)
                            currentInputConnection?.commitText(secondary, 1)
                            true
                        }
                    }
                    rowLayout.addView(keyView)
                }
            }
        }
    }

    private fun updatePredictions() {
        if (currentWord.isEmpty()) {
            pred1.text = "The"
            pred2.text = "I"
            pred3.text = "If"
            return
        }

        val matches = dictionary.filter { it.startsWith(currentWord.lowercase()) && it != currentWord.lowercase() }.take(3)
        pred1.text = matches.getOrNull(0) ?: ""
        pred2.text = matches.getOrNull(1) ?: ""
        pred3.text = matches.getOrNull(2) ?: ""
    }

    private fun selectPrediction(word: String) {
        if (word.isEmpty()) return
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(currentWord.length, 0)?.toString() ?: ""
        if (textBefore.lowercase() == currentWord.lowercase()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }
        ic.commitText("$word ", 1)
        currentWord = ""
        updatePredictions()
    }

    private fun haptic(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
