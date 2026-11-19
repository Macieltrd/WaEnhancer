package com.wmods.wppenhacer.ui.fragments.base

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat
import com.wmods.wppenhacer.App
import com.wmods.wppenhacer.BuildConfig
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.preference.FloatSeekBarPreference
import com.wmods.wppenhacer.xposed.utils.Utils
import rikka.material.preference.MaterialSwitchPreference
import java.util.Locale
import java.util.Objects

abstract class BasePreferenceFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    @JvmField
    protected var mPrefs: SharedPreferences? = null
    private val prefs: SharedPreferences
        get() = checkNotNull(mPrefs)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        prefs.registerOnSharedPreferenceChangeListener(this)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (parentFragmentManager.backStackEntryCount > 0) {
                        parentFragmentManager.popBackStack()
                    } else {
                        requireActivity().finish()
                    }
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        applyReferencePreferenceStyle(preferenceScreen, R.drawable.ic_general)
        updatePreferenceStates(null)
        monitorPreference()
        return super.onCreateView(inflater, container, savedInstanceState)!!
    }

    private fun applyReferencePreferenceStyle(group: PreferenceGroup?, inheritedIcon: Int) {
        if (group == null) return

        for (index in 0 until group.preferenceCount) {
            val preference = group.getPreference(index)
            if (preference is PreferenceCategory) {
                val categoryIcon = getSectionIcon(preference.title, inheritedIcon)
                if (preference.icon == null) preference.setIcon(categoryIcon)
                preference.isIconSpaceReserved = true
                preference.layoutResource = R.layout.preference_card_category
                preference.isSelectable = false
                applyReferencePreferenceStyle(preference, categoryIcon)
                continue
            }

            if (preference.icon == null) {
                preference.setIcon(getPreferenceIcon(preference, inheritedIcon))
            }
            preference.isIconSpaceReserved = true

            when (preference) {
                is MaterialSwitchPreference -> {
                    preference.layoutResource = R.layout.preference_feature_switch
                    preference.widgetLayoutResource = R.layout.preference_widget_material3_switch
                }

                is FloatSeekBarPreference -> preference.layoutResource =
                    R.layout.preference_feature_seekbar

                is SeekBarPreference -> preference.layoutResource =
                    R.layout.preference_feature_standard_seekbar

                is ColorPreferenceCompat -> preference.layoutResource =
                    R.layout.preference_feature_color

                else -> preference.layoutResource = R.layout.preference_feature_list
            }
        }
    }

    private fun getSectionIcon(title: CharSequence?, fallback: Int): Int {
        val value = normalize(title)
        return when {
            value.contains("privacy") || value.contains("privacidade") -> R.drawable.ic_privacy
            value.contains("media") || value.contains("midia") || value.contains("video") ||
                    value.contains("image") || value.contains("download") || value.contains("audio") -> R.drawable.ic_media

            value.contains("call") || value.contains("chamada") || value.contains("gravacao") -> R.drawable.ic_recording
            value.contains("status") || value.contains("home") || value.contains("inicio") -> R.drawable.ic_home_black_24dp
            value.contains("custom") || value.contains("personal") || value.contains("personalizacao") ->
                R.drawable.ic_dashboard_black_24dp

            value.contains("conversation") || value.contains("conversa") || value.contains("general") ||
                    value.contains("geral") -> R.drawable.ic_general

            else -> fallback
        }
    }

    private fun getPreferenceIcon(preference: Preference, fallback: Int): Int {
        val value = "${normalize(preference.key)} ${normalize(preference.title)}"
        return when {
            value.contains("privacy") || value.contains("privacidade") || value.contains("archive") ||
                    value.contains("ghost") || value.contains("freeze") || value.contains("read") -> R.drawable.ic_privacy

            value.contains("record") || value.contains("grav") -> R.drawable.ic_recording
            value.contains("media") || value.contains("video") || value.contains("image") ||
                    value.contains("audio") || value.contains("download") || value.contains("transcription") -> R.drawable.ic_media

            value.contains("color") || value.contains("theme") || value.contains("wallpaper") ||
                    value.contains("bubble") || value.contains("css") || value.contains("animation") ->
                R.drawable.ic_dashboard_black_24dp

            value.contains("status") || value.contains("home") || value.contains("inicio") -> R.drawable.ic_home_black_24dp
            else -> fallback
        }
    }

    private fun normalize(value: CharSequence?): String = value?.toString()
        ?.lowercase(Locale.ROOT)
        ?.replace('í', 'i')
        ?.replace('ç', 'c')
        ?.replace('ã', 'a')
        ?.replace('á', 'a')
        ?.replace('é', 'e')
        ?.replace('ê', 'e')
        ?.replace('ó', 'o')
        ?.replace('ú', 'u')
        .orEmpty()

    override fun onResume() {
        super.onResume()
        setDisplayHomeAsUpEnabled(true)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        App.instance.sendBroadcast(Intent("${BuildConfig.APPLICATION_ID}.MANUAL_RESTART"))
        if (isAdded) updatePreferenceStates(key)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPrefs?.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun setPreferenceState(key: String, enabled: Boolean) {
        val preference = findPreference<Preference>(key) ?: return
        preference.isEnabled = enabled
        if (preference is MaterialSwitchPreference && !enabled) {
            preference.isChecked = false
        }
    }

    private fun monitorPreference() {
        findPreference<MaterialSwitchPreference>("downloadstatus")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue -> checkStoragePermission(newValue) }

        findPreference<MaterialSwitchPreference>("downloadviewonce")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue -> checkStoragePermission(newValue) }

        findPreference<MaterialSwitchPreference>("force_disable_emojis")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue is Boolean && newValue) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.force_disable_emojis_alert_title)
                        .setMessage(R.string.force_disable_emojis_alert_msg)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                true
            }
    }

    private fun checkStoragePermission(newValue: Any?): Boolean {
        if (newValue is Boolean && newValue) {
            val needsPermission =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) ||
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                                ContextCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) != PackageManager.PERMISSION_GRANTED)
            if (needsPermission) {
                App.showRequestStoragePermission(requireActivity())
                return false
            }
        }
        return true
    }

    @SuppressLint("ApplySharedPref")
    private fun updatePreferenceStates(key: String?) {
        val changeColorEnabled = prefs.getBoolean("changecolor", false)
        val changeColorMode = prefs.getString("changecolor_mode", "manual")
        val monetAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val useMonetColors =
            changeColorEnabled && monetAvailable && Objects.equals(changeColorMode, "monet")

        setPreferenceState("changecolor_mode", changeColorEnabled && monetAvailable)
        setPreferenceState("primary_color", changeColorEnabled && !useMonetColors)
        setPreferenceState("background_color", changeColorEnabled && !useMonetColors)
        setPreferenceState("text_color", changeColorEnabled && !useMonetColors)

        if (key == "thememode") {
            App.setThemeMode(prefs.getString("thememode", "0")!!.toInt())
        }

        val colorMode = prefs.getString("wae_color_mode", "preset")
        val useMonet =
            Objects.equals(colorMode, "monet") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        setPreferenceState("wae_color_preset", !useMonet)

        if (key == "wae_color_mode" || key == "wae_color_preset") {
            activity?.recreate()
        }

        if (key == "force_english") {
            prefs.edit().commit()
            context?.let(Utils::doRestart)
        }

        val igstatus = prefs.getBoolean("igstatus", false)
        setPreferenceState("oldstatus", !igstatus)

        val oldstatus = prefs.getBoolean("oldstatus", false)
        setPreferenceState("verticalstatus", !oldstatus)
        setPreferenceState("channels", !oldstatus)
        setPreferenceState("removechannel_rec", !oldstatus)
        setPreferenceState("status_style", !oldstatus)
        setPreferenceState("igstatus", !oldstatus)

        val channels = prefs.getBoolean("channels", false)
        setPreferenceState("removechannel_rec", !channels && !oldstatus)

        val freezelastseen = prefs.getBoolean("freezelastseen", false)
        setPreferenceState("show_freezeLastSeen", !freezelastseen)
        setPreferenceState("showonlinetext", !freezelastseen)
        setPreferenceState("dotonline", !freezelastseen)

        val separategroups = prefs.getBoolean("separategroups", false)
        setPreferenceState("filtergroups", !separategroups)

        val filtergroups = prefs.getBoolean("filtergroups", false)
        setPreferenceState("separategroups", !filtergroups)

        val floatingBottomBar = prefs.getBoolean("floating_bottom_bar", false)
        setPreferenceState("native_floating_navigation", !floatingBottomBar)

        val nativeFloatingNavigation = prefs.getBoolean("native_floating_navigation", false)
        setPreferenceState("floating_bottom_bar", !nativeFloatingNavigation)

        val callBlockContacts = findPreference<Preference>("call_block_contacts")
        val callWhiteContacts = findPreference<Preference>("call_white_contacts")
        if (callBlockContacts != null && callWhiteContacts != null) {
            when (prefs.getString("call_privacy", "0")!!.toInt()) {
                3 -> {
                    callBlockContacts.isEnabled = true
                    callWhiteContacts.isEnabled = false
                }

                4 -> {
                    callWhiteContacts.isEnabled = true
                    callBlockContacts.isEnabled = false
                }

                else -> {
                    callWhiteContacts.isEnabled = false
                    callBlockContacts.isEnabled = false
                }
            }
        }
    }

    fun setDisplayHomeAsUpEnabled(enabled: Boolean) {
        val act = activity as? AppCompatActivity ?: return
        val actionBar = act.supportActionBar ?: return
        // Sem seta de voltar em nenhuma tela: o logo fica sempre no mesmo lugar,
        // alinhado com a borda esquerda dos cartões (16dp). Voltar = botão/gesto do Android.
        actionBar.setDisplayHomeAsUpEnabled(false)

        act.findViewById<android.view.View>(com.wmods.wppenhacer.R.id.toolbar)?.let { tb ->
            val d = tb.resources.displayMetrics.density
            val margin = (16 * d).toInt()   // logo alinhado com a borda esquerda dos cartões
            val endFix = (4.5f * d).toInt()    // ícones da direita alinhados com a borda direita dos cartões
            tb.setPaddingRelative(margin, tb.paddingTop, endFix, tb.paddingBottom)
        }
    }


    // ---------- Seções que recolhem/expandem ao tocar no título ----------
    // Movimento igual ao do outro app: as linhas encolhem e somem (fade) ao mesmo tempo,
    // e o que está embaixo sobe junto. Ao abrir, crescem e surgem ao mesmo tempo.

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Quem anima é o código abaixo; o animador padrão da lista atrapalharia
        listView?.itemAnimator = null
    }

    // v2: todas as seções começam fechadas (e o app lembra o que você abrir depois)
    private val sectionPrefs by lazy {
        requireContext().getSharedPreferences("ui_sections_v2", Context.MODE_PRIVATE)
    }

    private val rowMargins = java.util.WeakHashMap<View, Int>()
    private val sectionAnimators = HashMap<PreferenceCategory, android.animation.ValueAnimator>()
    private var sectionAdapter: PreferenceGroupAdapter? = null

    private fun sectionKey(category: PreferenceCategory) =
        "${javaClass.simpleName}:${category.title}"

    private fun isSectionCollapsed(category: PreferenceCategory): Boolean =
        sectionPrefs.getBoolean(sectionKey(category), true)

    /** Telas com poucas opções podem desligar o recolhimento das seções. */
    protected open fun hasCollapsibleSections(): Boolean = true

    private fun sectionOf(preference: Preference?): PreferenceCategory? {
        val parent = preference?.parent
        return if (parent is PreferenceCategory && parent.parent === preferenceScreen) parent else null
    }

    /** Aplica, sem animação, o estado "fechado" ou "aberto" a uma linha. */
    private fun applyRowState(view: View, collapsed: Boolean) {
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val margin = rowMargins.getOrPut(view) { lp.bottomMargin }
        if (collapsed) {
            lp.height = 0
            lp.bottomMargin = 0
            view.alpha = 0f
        } else {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.bottomMargin = margin
            view.alpha = 1f
        }
    }

    private fun toggleSection(category: PreferenceCategory, collapsed: Boolean) {
        sectionPrefs.edit().putBoolean(sectionKey(category), collapsed).apply()
        sectionAnimators.remove(category)?.cancel()
        val list = listView ?: return
        val adapter = sectionAdapter ?: return

        val rows = ArrayList<View>()
        for (i in 0 until list.childCount) {
            val child = list.getChildAt(i)
            val pos = list.getChildAdapterPosition(child)
            if (pos == RecyclerView.NO_POSITION) continue
            if (adapter.getItem(pos)?.parent === category) rows.add(child)
        }
        if (rows.isEmpty()) return

        val widthAvail = list.width - list.paddingLeft - list.paddingRight
        val n = rows.size
        val startH = IntArray(n)
        val endH = IntArray(n)
        val startM = IntArray(n)
        val endM = IntArray(n)
        val startA = FloatArray(n)
        val endA = if (collapsed) 0f else 1f
        for (i in 0 until n) {
            val r = rows[i]
            val lp = r.layoutParams as ViewGroup.MarginLayoutParams
            val margin = rowMargins.getOrPut(r) { lp.bottomMargin }
            r.measure(
                View.MeasureSpec.makeMeasureSpec(widthAvail - lp.marginStart - lp.marginEnd, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            startH[i] = r.height
            startM[i] = lp.bottomMargin
            startA[i] = r.alpha
            endH[i] = if (collapsed) 0 else r.measuredHeight
            endM[i] = if (collapsed) 0 else margin
        }

        val anim = android.animation.ValueAnimator.ofFloat(0f, 1f)
        anim.duration = 250
        anim.interpolator = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
        var cancelled = false
        anim.addUpdateListener { a ->
            val p = a.animatedValue as Float
            // ao fechar, o fade termina um pouco antes (como no outro app)
            val pa = if (collapsed) minOf(1f, p * 1.25f) else p
            for (i in 0 until n) {
                val r = rows[i]
                if (!r.isAttachedToWindow) continue
                val lp = r.layoutParams as ViewGroup.MarginLayoutParams
                lp.height = (startH[i] + (endH[i] - startH[i]) * p).toInt()
                lp.bottomMargin = (startM[i] + (endM[i] - startM[i]) * p).toInt()
                r.alpha = startA[i] + (endA - startA[i]) * pa
                r.layoutParams = lp
            }
        }
        anim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: android.animation.Animator) {
                cancelled = true
            }

            override fun onAnimationEnd(animation: android.animation.Animator) {
                sectionAnimators.remove(category)
                if (cancelled) return
                for (r in rows) {
                    applyRowState(r, collapsed)
                    r.requestLayout()
                }
            }
        })
        sectionAnimators[category] = anim
        anim.start()
    }

    private fun expandAllSections() {
        val screen = preferenceScreen ?: return
        var changed = false
        for (index in 0 until screen.preferenceCount) {
            val category = screen.getPreference(index) as? PreferenceCategory ?: continue
            if (isSectionCollapsed(category)) {
                sectionAnimators.remove(category)?.cancel()
                sectionPrefs.edit().putBoolean(sectionKey(category), false).apply()
                changed = true
            }
        }
        if (changed) sectionAdapter?.notifyDataSetChanged()
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        val adapter = object : PreferenceGroupAdapter(preferenceScreen) {
            override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                if (!hasCollapsibleSections()) return
                val item = getItem(position)

                if (item is PreferenceCategory && item.parent === preferenceScreen) {
                    val chevron = holder.itemView.findViewById<android.widget.ImageView>(R.id.section_chevron)
                    chevron?.visibility = View.VISIBLE
                    chevron?.rotation = if (isSectionCollapsed(item)) 0f else 180f
                    holder.itemView.isClickable = true
                    holder.itemView.setOnClickListener {
                        val collapsed = !isSectionCollapsed(item)
                        chevron?.animate()?.rotation(if (collapsed) 0f else 180f)?.setDuration(250)?.start()
                        toggleSection(item, collapsed)
                    }
                    return
                }

                val section = sectionOf(item)
                if (section != null && !sectionAnimators.containsKey(section)) {
                    applyRowState(holder.itemView, isSectionCollapsed(section))
                }
            }
        }
        sectionAdapter = adapter
        return adapter
    }

    /** Scroll to a preference by its key and briefly highlight it. */
    override fun scrollToPreference(preferenceKey: String) {
        expandAllSections()
        view?.postDelayed({
            val preference = findPreference<Preference>(preferenceKey) ?: return@postDelayed
            super.scrollToPreference(preference)
            highlightPreference(preference)
        }, 100)
    }

    private fun highlightPreference(preference: Preference?) {
        view?.postDelayed({
            val recyclerView = listView ?: return@postDelayed
            val targetKey = preference?.key ?: return@postDelayed
            var found = false

            for (index in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(index)
                val holder = recyclerView.getChildViewHolder(child)
                if (holder is androidx.preference.PreferenceViewHolder) {
                    val position = holder.bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        try {
                            val item = findPreferenceAtPosition(preferenceScreen, position)
                            if (item?.key == targetKey) {
                                animateHighlight(holder.itemView)
                                found = true
                                break
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            if (!found) view?.postDelayed({ tryHighlightAgain(targetKey) }, 500)
        }, 500)
    }

    private fun tryHighlightAgain(targetKey: String) {
        val recyclerView = listView ?: return
        for (index in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(index)
            if (child is ViewGroup) {
                val holder = recyclerView.getChildViewHolder(child)
                if (holder is androidx.preference.PreferenceViewHolder) {
                    val position = holder.bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val preference = findPreferenceAtPosition(preferenceScreen, position)
                        if (preference?.key == targetKey) {
                            animateHighlight(child)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun findPreferenceAtPosition(
        group: PreferenceGroup?,
        targetPosition: Int
    ): Preference? {
        if (group == null) return null

        var currentPosition = 0
        for (index in 0 until group.preferenceCount) {
            val preference = group.getPreference(index) ?: continue
            if (currentPosition == targetPosition) return preference
            currentPosition++

            if (preference is PreferenceGroup) {
                val childCount = countPreferences(preference)
                if (targetPosition < currentPosition + childCount) {
                    return findPreferenceAtPosition(preference, targetPosition - currentPosition)
                }
                currentPosition += childCount
            }
        }
        return null
    }

    private fun countPreferences(group: PreferenceGroup): Int {
        var count = 0
        for (index in 0 until group.preferenceCount) {
            val preference = group.getPreference(index)
            if (preference is PreferenceGroup) {
                count += countPreferences(preference)
            } else {
                count++
            }
        }
        return count
    }

    private fun animateHighlight(view: View?) {
        if (view == null || context == null) return

        val typedValue = TypedValue()
        view.context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        val highlightColor = android.graphics.Color.argb(
            51,
            android.graphics.Color.red(primaryColor),
            android.graphics.Color.green(primaryColor),
            android.graphics.Color.blue(primaryColor)
        )
        val originalBackground = view.background
        view.setBackgroundColor(highlightColor)
        view.postDelayed({
            if (originalBackground != null) {
                view.background = originalBackground
            } else {
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }, 1500)
    }
}
