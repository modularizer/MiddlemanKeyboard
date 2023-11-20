package com.example.middlemankeyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import io.ktor.util.toUpperCasePreservingASCIIRules

import kotlinx.coroutines.*

// import JSONObject from org.json

/* Make an abstract class for text suggestions. This will take in the input text element and the
 * text suggestions element. It will attach a change handler to the input text element which will
 * call the API and update the text suggestions element.
 */



data class CursorInfo(val mode: String, val startPosition: Int?, val endPosition: Int?, val selectedText: String)



abstract class TextTransformerABC {
    private var content: String = ""
    abstract fun setContent(text: String)

    abstract suspend fun  transform(): String

    abstract suspend fun getWordSuggestions(): Triple<String, String, String>
}


class TextTransformer : TextTransformerABC() {
    private var content: String = ""

    override public fun setContent(text: String) {
        content = text
    }

   override suspend fun transform(): String {
        return content
    }

    override suspend fun getWordSuggestions(): Triple<String, String, String> {
        return Triple("word one", "word two", "word three")
    }
}



class KeyboardService : InputMethodService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }


    // Define a mapping of keys to rows
    private val transformer = TextTransformer()

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


    private lateinit var suggestionTextElement: TextView
    private lateinit var wordSuggestionElements: Triple<TextView, TextView, TextView>


    override fun onCreateInputView(): View {
        // Initialize the keyboard layout
        return setKeyboardLayout(currentKeyboard)
    }

    public fun switchKeyboard(keyboardName: String, temporary: Boolean = false) {
        if (temporary) {
            nextKeyboard = currentKeyboardName
        }else{
            nextKeyboard = null
        }
        currentKeyboardName = keyboardName
        currentKeyboard = keyboards[currentKeyboardName]!!
        setInputView(onCreateInputView())

    }

    private fun setKeyboardLayout(keyboard: Array<Array<String>>): LinearLayout {
        val baseLayout = layoutInflater.inflate(R.layout.keyboard_layout, null) as LinearLayout
        suggestionTextElement = baseLayout.findViewById<TextView>(R.id.suggestion_text)
        suggestionTextElement.movementMethod = ScrollingMovementMethod.getInstance()
        wordSuggestionElements = Triple(
            baseLayout.findViewById<TextView>(R.id.word_suggestion_1),
            baseLayout.findViewById<TextView>(R.id.word_suggestion_2),
            baseLayout.findViewById<TextView>(R.id.word_suggestion_3)
        )

        val rowLayouts = makeKeyboardLayout(keyboard)
        // set the content of the keyboard_rows layout to the rowLayouts
        val keyboardRows = baseLayout.findViewById<LinearLayout>(R.id.keyboard_rows)
        keyboardRows.removeAllViews()
        rowLayouts.forEach { rowLayout ->
            keyboardRows.addView(rowLayout)
        }
        return baseLayout
    }

    private fun makeKeyboardLayout(keyboard: Array<Array<String>>): Array<LinearLayout> {
        val rowLayouts = arrayOfNulls<LinearLayout>(keyboard.size)
        var ind = 0
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
                        "⌫" -> handleBackspace()
                        "↵" -> handleEnter()
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

            rowLayouts[ind] = rowLayout
            ind += 1
        }
        return rowLayouts!! as Array<LinearLayout>
    }

    private fun inputText(text: String) {
        val inputConnection = currentInputConnection
        inputConnection?.commitText(text, 1)
        afterTextChange()
        if (nextKeyboard != null) {
            switchKeyboard(nextKeyboard!!)
        }
    }

    private fun handleEnter() {
        val editorInfo = currentInputEditorInfo
        nextKeyboard = null

        if (editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0) {
            // Multi-line text field, insert newline
            inputText("\n")
        } else {
            // Single-line text field, submit text
            submit()
        }
    }

    private fun handleBackspace() {
        nextKeyboard = null
        val cursorInfo = getCursorInfo(true)

        val inputConnection = currentInputConnection
        val noSelection = cursorInfo.startPosition == cursorInfo.endPosition
        if (noSelection) inputConnection?.deleteSurroundingText(1, 0) else inputConnection?.commitText("", 0)

        afterTextChange()
    }

    public fun clearText() {
        // use inputConnection to delete all text in the text field
        nextKeyboard = null
        val inputConnection = currentInputConnection
        inputConnection?.deleteSurroundingText(getText().length, 0)

        afterTextChange()
    }

    public fun afterTextChange() {
        getSuggestions()
    }

    private fun getSuggestions() {
        getWordSuggestions()
        getFullSuggestion()
    }

    private fun getFullSuggestion() {
        val text = getText()
        transformer.setContent(text)

        // asyncronously get the full suggestion text
        // Start the coroutine
        serviceScope.launch(Dispatchers.IO) {
            try {
                val suggestionText = transformer.transform()
                withContext(Dispatchers.Main) {
                    suggestionTextElement.setText(suggestionText)
                }
            } catch (e: Exception) {
                Log.e("KeyboardService", "Error: ${e.message}")
                // Handle the exception or notify the user
            }
        }
    }
    private fun getWordSuggestions() {
        val text = getText()
        transformer.setContent(text)

        // asyncronously get the word suggestions
        // Start the coroutine
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Do the work in a background thread
                val wordSuggestions = transformer.getWordSuggestions()
                // Switch to the main thread
                withContext(Dispatchers.Main) {
                    // Update the UI
                    wordSuggestionElements.first.setText(wordSuggestions.first)
                    wordSuggestionElements.second.setText(wordSuggestions.second)
                    wordSuggestionElements.third.setText(wordSuggestions.third)
                }
            } catch (e: Exception) {
                Log.e("KeyboardService", "Error: ${e.message}")
                // Handle the exception or notify the user
            }
        }
    }

    public fun getCursorInfo(backspace: Boolean = false): CursorInfo {
        val inputConnection = currentInputConnection
        val request = ExtractedTextRequest()
        val extractedText = inputConnection?.getExtractedText(request, 0)
        val startPosition = extractedText?.selectionStart
        val endPosition = extractedText?.selectionEnd
        val length = extractedText?.text.toString().length

        val noSelection = startPosition == endPosition
        val startCursorAtEnd = startPosition == length
        val selectedText = getSelectedText()
        val mode = if (backspace){
            if (noSelection && startCursorAtEnd) "pop" else if (noSelection) "backspace" else "remove"
        }else{
            if (noSelection && startCursorAtEnd) "append" else if (noSelection) "insert" else "replace"
        }

        // return mode, start position, end position, and selected text as a tuple
        return CursorInfo(mode, startPosition, endPosition, selectedText)
    }

    public fun setCursor(startPosition: Int = -1, endPosition: Int? = null) {
        // -1 means end of text, null means no selection
        val inputConnection = currentInputConnection
        var start = startPosition
        if (startPosition == -1){
            start = getText().length
        }
        var end = endPosition
        if (endPosition == -1){
            end = getText().length
        }
        if (endPosition == null){
            end = start
        }
        inputConnection?.setSelection(start, end!!)
    }

    public fun getSelectedText(): String {
        val inputConnection = currentInputConnection
        val selectedText = inputConnection?.getSelectedText(0)
        return selectedText.toString()
    }

    public fun getText(): String {
        val inputConnection = currentInputConnection
        val request = ExtractedTextRequest()
        val extractedText = inputConnection?.getExtractedText(request, 0)
        return extractedText?.text.toString()
    }

    public fun setText(text: String, cursorPosition: Int = -1, endPosition: Int? = null) {
        val inputConnection = currentInputConnection
        inputConnection?.beginBatchEdit()

        // Move the cursor to the start of the text
        inputConnection?.setSelection(0, 0)

        // Get the total length of the current text
        val extractedText = inputConnection?.getExtractedText(ExtractedTextRequest(), 0)
        val length = extractedText?.text?.length ?: 0

        // Delete the entire text
        inputConnection?.deleteSurroundingText(0, length)

        // Insert the new text
        inputConnection?.commitText(text, 1)

        if (cursorPosition != -1){
            setCursor(cursorPosition, endPosition)
        }

        inputConnection?.endBatchEdit()
    }

    public fun submit() {
        // Implement logic to submit the text
        currentInputConnection?.performEditorAction(1)
    }

}
