package com.example.middlemankeyboard

import android.inputmethodservice.InputMethodService
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout


abstract class TextTransformer {
    public var rawText: String = ""
    public var transformedText: String = ""

    public fun reset(): String {
        return transform("")
    }

    public fun addText(text: String, index: Int? = null): String {
        if (index == null) {
            rawText += text
        } else {
            rawText = rawText.substring(0, index) + text + rawText.substring(index)
        }
        transformedText = transform(rawText)
        return transformedText
    }

    public fun removeText(length: Int, index: Int? = null): String {
        if (index == null) {
            rawText = rawText.dropLast(length)
        } else {
            rawText = rawText.substring(0, index) + rawText.substring(index + length)
        }
        return transform(rawText)
    }

    public fun transform(newRawText: String): String {
        val oldRawText = rawText
        rawText = newRawText
        transformedText = transformText(newRawText, oldRawText)
        return transformedText
    }

    abstract fun transformText(newRawText: String, oldRawText: String? = null): String
}


class sPOngEbObTRaNSfOrMer : TextTransformer() {
    override fun transformText(newRawText: String, oldRawText: String?): String {
        return newRawText.map { if (Math.random() < 0.4) it.uppercaseChar() else it }.joinToString("")
    }
}

class NormalTransformer : TextTransformer() {
    override fun transformText(newRawText: String, oldRawText: String?): String {
        return newRawText
    }
}


class KeyboardService : InputMethodService() {
    // Define a mapping of keys to rows
    private val transformer: TextTransformer? = sPOngEbObTRaNSfOrMer();

    private val emojis = "👍😎💩"
    private val lock = "\uD83D\uDD12"


    private val abcLayout = arrayOf(
        arrayOf("#", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", " ⌫ "),
        arrayOf("@", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", ),
        arrayOf(lock, "a", "s", "d", "f", "g", "h", "j", "k", "l", "'", " ↵"),
        arrayOf("  ⇧ ", "z", "x", "c", "v", "b", "n", "m", ",", ".", "?", "!" ),
        arrayOf("123%","(", "                  ",  ")",  emojis)
        // Add other special keys or rows as needed
    )
    private val capsLayout = arrayOf(
        arrayOf("@", "#", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", " ⌫ "),
        arrayOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "(", ")"),
        arrayOf(lock, "A", "S", "D", "F", "G", "H", "J", "K", "L", "\"", " ↵ "),
        arrayOf("  ⇧ ", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "?", "!" ),
        arrayOf("123%", "👍", "                     ", ":)",  emojis)
        // Add other special keys or rows as needed
    )
    // Define rows of keys
    private val numLayout = arrayOf(
        arrayOf("#", "^", "!", "/", "*", "⌫"),
        arrayOf("$", "7", "8", "9", "%", "~"),
        arrayOf("(", "4", "5", "6", "+", ")"),
        arrayOf("[", "1", "2", "3", "-", "]"),
        arrayOf("abc", " 0 ", " . ", " = ", emojis + "  ")

        // Add other special keys or rows as needed
    )
    private val emojiLayout = arrayOf(
        // Row 1: Most Popular Emotions & Gestures
        arrayOf("😂", "😍", "😎", "💀", "😇", "😢", "🥳", "😡", "⌫"),

        // Row 2: More weird faces and expressions
        arrayOf("😈", "🤡", "🤠", "🤑", "🤓", "🤖", "👽", "👾", "👻"),

        // Row 2: Activities & Celebrations
        arrayOf(":)", "👀", "🙈", "💩", "💃",  "🎉", "👅", "🚴‍♀️", "!"),

        // Row 3: Animals, Nature & Weather
        arrayOf("<3", "🚗", "🐱", "😤", "🙏", "🌳", "🔥", "❄️", "?"),

        // Row 4: Food, Objects & Symbols
        arrayOf("123%", "🍕", "               ",  "👍", "❤️", "abc")
    )

    private val keyboards = mapOf(
        "abc" to abcLayout,
        lock to capsLayout,
        "123%" to numLayout,
        emojis to emojiLayout
    )

    private var currentKeyboardName = "abc"
    private var currentKeyboard = keyboards[currentKeyboardName]!!
    private var nextKeyboard: String? = null

    private var textArea: EditText? = null


    override fun onCreateInputView(): View {

        // Initialize the keyboard layout
        return makeKeyboardLayout(currentKeyboard)
    }


    private fun switchKeyboard(keyboardName: String, temporary: Boolean = false) {
        if (temporary) {
            nextKeyboard = currentKeyboardName
        }else{
            nextKeyboard = null
        }
        currentKeyboardName = keyboardName
        currentKeyboard = keyboards[currentKeyboardName]!!
        setInputView(onCreateInputView())

    }

    private fun makeKeyboardLayout(keyboard: Array<Array<String>>): LinearLayout {
        val baseLayout = layoutInflater.inflate(R.layout.keyboard_layout, null) as LinearLayout
        // get the text area from the layout, id is typed_text_area
        // Find the EditText within the inflated layout
        textArea = baseLayout.findViewById(R.id.typed_text_area) as EditText

        // set a change handler on the text area which will set the input connection text
        textArea?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                setText(transformer.transform(s.toString()))
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // prevent any edits
            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing
            }
        })
        initializeTextArea()


        keyboard.forEach { row ->
            val rowLayout = LinearLayout(this)
            rowLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            rowLayout.orientation = LinearLayout.HORIZONTAL

            // Calculate the total weight for the row based on the length of the labels
            val totalWeight = row.map { it.length }.sum().toFloat()
            row.forEach { keyLabel ->
                val key = Button(this)
                key.text = keyLabel
                // Set the text size of the button
                key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f) // 18sp font size, change as needed

                key.setOnClickListener {
                    val trimmedLabel = keyLabel.trim()
                    when (trimmedLabel) {
                        "⇧" -> switchKeyboard(if (currentKeyboardName == lock) "abc" else lock, currentKeyboardName == "abc")
                        "123%" -> switchKeyboard("123%")
                        "abc" -> switchKeyboard("abc")
                        lock -> switchKeyboard(if (currentKeyboardName == lock) "abc" else lock)
                        emojis -> switchKeyboard(emojis)
                        "→|" -> inputText('\t'.toString())
                        "⌫" -> handleBackspace()
                        "↵" -> inputText("\n")
                        "" -> inputText(" ")
                        else -> inputText(keyLabel)
                    }
                }

                // Set OnLongClick Listener
                key.setOnLongClickListener {
                    val trimmedLabel = keyLabel.trim()
                    when (trimmedLabel) {
                        "⇧" -> Unit
                        "123%" -> Unit
                        "abc" -> Unit
                        lock -> Unit
                        emojis -> Unit
                        "⌫" -> clearText()
                        "↵" -> Unit
                        else -> switchKeyboard(if (currentKeyboardName == lock) "abc" else lock, true)
                    }
                    true
                }
                key.setPadding(0, 0, 0, 0)

                // Calculate weight for each key based on its label length
                val keyWeight =  (2 + keyLabel.length.toFloat()) / totalWeight
                val keyLayoutParams = LinearLayout.LayoutParams(0, 130, keyWeight)
                keyLayoutParams.setMargins(-4, -4, -4, -4)
                key.layoutParams = keyLayoutParams

                rowLayout.addView(key)
            }

            baseLayout.addView(rowLayout)
        }
        return baseLayout
    }


    private fun inputText(text: String) {
        if (!cursorAtEnd()){
            return
        }
        val inputConnection = currentInputConnection
        val selectedText = inputConnection?.getSelectedText(0)
        if (selectedText.isNullOrEmpty()) {
            // add text directly to end of text area do not use transformer
            textArea?.setText(textArea?.text.toString() + text)
            textArea?.setSelection(textArea?.text.toString().length)


            if (nextKeyboard != null) {
                switchKeyboard(nextKeyboard!!)
            }
        }else{
            // no support yet for deleting selected text
        }

    }

    private fun handleBackspace() {
        if (!cursorAtEnd()){
            return
        }
        val inputConnection = currentInputConnection
        val selectedText = inputConnection?.getSelectedText(0)

        if (selectedText.isNullOrEmpty()) {
            // remove last character from text area do not use transformer
            val text = textArea?.text.toString()
            textArea?.setText(text.substring(0, text.length - 1))
            textArea?.setSelection(textArea?.text.toString().length)
        } else {
            // no support yet for deleting selected text
        }
    }

    private fun clearText() {
        if (!cursorAtEnd()){
            return
        }
        val inputConnection = currentInputConnection
        val selectedText = inputConnection?.getSelectedText(0)
        if (selectedText.isNullOrEmpty()) {
            textArea?.setText("")
            textArea?.setSelection(textArea?.text.toString().length)
        } else {
            // no support yet for deleting selected text
        }
    }

    private fun initializeTextArea() {
        val initialRawText = getText()
        textArea?.setText(initialRawText)
        textArea?.setSelection(textArea?.text.toString().length)
    }

    private fun getText(): String {
        val inputConnection = currentInputConnection
        val request = ExtractedTextRequest()
        val extractedText = inputConnection?.getExtractedText(request, 0)
        return extractedText?.text.toString()
    }

    private fun setText(text: String) {
        val inputConnection = currentInputConnection
        inputConnection?.setComposingText(text, 1)
    }

    private fun cursorAtEnd(): Boolean {
        val inputConnection = currentInputConnection
        val request = ExtractedTextRequest()
        val extractedText = inputConnection?.getExtractedText(request, 0)
        // get true if cursor is at end of text AND there is no selected text
        val atEnd = extractedText?.selectionStart == extractedText?.selectionEnd && extractedText?.selectionStart == extractedText?.text.toString().length

        if (!atEnd){
            // move cursor to end
            inputConnection?.setSelection(extractedText?.text.toString().length, extractedText?.text.toString().length)
        }
        return atEnd
    }


}
