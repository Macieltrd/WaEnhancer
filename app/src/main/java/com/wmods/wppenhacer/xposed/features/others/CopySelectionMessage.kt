package com.wmods.wppenhacer.xposed.features.others

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.components.AlertDialogWpp
import com.wmods.wppenhacer.xposed.features.listeners.ConversationItemListener
import com.wmods.wppenhacer.xposed.features.providers.ContextMenuActionProvider
import com.wmods.wppenhacer.xposed.utils.DesignUtils
import com.wmods.wppenhacer.xposed.utils.ModuleContextWrapper
import com.wmods.wppenhacer.xposed.utils.Utils

class CopySelectionMessage(classLoader: ClassLoader, xprefs: SharedPreferences) :
    Feature(classLoader, xprefs) {

    override fun doHook() {
        if (!xprefs.getBoolean("copy_selection_message", false)) return


        // Botão 1: copia a mensagem inteira direto, sem abrir janela
        ContextMenuActionProvider.register { activity, _, wpp ->
            val messageText = wpp.messageStr ?: ""
            if (messageText.isEmpty()) return@register null

            ContextMenuActionProvider.ContextMenuAction(
                title = Utils.getString(R.string.copy_message_action)
            ) {
                val view = ConversationItemListener.listItems.entries.firstOrNull {
                    it.value.messageId == wpp.key.messageID &&
                            ConversationItemListener.isViewBoundToMessage(it.key, wpp.key.messageID)
                }?.key
                val textView = view?.findViewById<TextView>(Utils.getID("message_text", "id"))
                copyToClipboard(activity, (textView?.text ?: messageText).toString())
            }
        }

        // Botão 2: abre a janela para escolher um pedaço ou editar antes de copiar
        ContextMenuActionProvider.register { activity, _, wpp ->
            val messageText = wpp.messageStr ?: ""
            if (messageText.isEmpty()) return@register null

            ContextMenuActionProvider.ContextMenuAction(
                title = Utils.getString(R.string.copy_selection_action)
            ) {
                val view = ConversationItemListener.listItems.entries.firstOrNull {
                    it.value.messageId == wpp.key.messageID &&
                            ConversationItemListener.isViewBoundToMessage(it.key, wpp.key.messageID)
                }?.key
                val textView = view?.findViewById<TextView>(Utils.getID("message_text", "id"))
                showSelectionDialog(activity, textView?.text ?: messageText)
            }
        }
    }

    private fun copyToClipboard(activity: Activity, text: String) {
        try {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
            if (xprefs.getBoolean("copy_message_toast", true)) {
                Utils.showToast(Utils.getString(R.string.message_copied), Toast.LENGTH_SHORT)
            }
        } catch (e: Throwable) {
            Utils.showToast(e.message ?: "", Toast.LENGTH_SHORT)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showSelectionDialog(activity: Activity, messageText: CharSequence) {
        val d = activity.resources.displayMetrics.density
        fun Int.dp() = (this * d).toInt()

        val ctx = ModuleContextWrapper(activity)

        val textInputLayout = TextInputLayout(
            ctx,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            isCounterEnabled = true
            hint = Utils.getString(R.string.message)
        }
        val editText = TextInputEditText(textInputLayout.context).apply {
            setText(messageText)
            textSize = 14f
            minLines = 3
            maxLines = 10
            gravity = Gravity.TOP
            setLineSpacing(0f, 1.4f)
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setSelection(0)
        }
        textInputLayout.addView(editText)

        val scrollView = NestedScrollView(ctx).apply {
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            addView(textInputLayout)
        }

        val textColor = DesignUtils.getPrimaryTextColor()
        val outlineColor = Color.argb(
            60,
            Color.red(textColor),
            Color.green(textColor),
            Color.blue(textColor)
        )

        fun newOutlinedButton(label: String) = MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            setTextColor(textColor)
            setStrokeColor(ColorStateList.valueOf(outlineColor))
            cornerRadius = Utils.dipToPixels(50f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = 8.dp() }
        }

        val copyButton = newOutlinedButton(Utils.getString(R.string.copy_selection_copy))
        val closeButton = newOutlinedButton(Utils.getString(R.string.close))

        val buttonsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12.dp() }
            addView(copyButton)
            addView(closeButton)
        }

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 8.dp(), 16.dp(), 16.dp())
            addView(scrollView)
            addView(buttonsRow)
        }

        val dialog = AlertDialogWpp(activity)
            .setTitle(Utils.getString(R.string.copy_selection_dialog_title))
            .setView(container)
            .create()

        copyButton.setOnClickListener {
            val full = editText.text?.toString() ?: ""
            val start = minOf(editText.selectionStart, editText.selectionEnd)
            val end = maxOf(editText.selectionStart, editText.selectionEnd)
            val toCopy = if (start >= 0 && end > start && end <= full.length) {
                full.substring(start, end)
            } else {
                full
            }
            copyToClipboard(activity, toCopy)
        }
        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun getPluginName(): String = "Copy Selection Message"
}
