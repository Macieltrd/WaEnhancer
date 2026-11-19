package com.wmods.wppenhacer.ui.fragments.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.wmods.wppenhacer.databinding.BaseFragmentBinding

open class BaseFragment : Fragment() {
    private var baseBinding: BaseFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = BaseFragmentBinding.inflate(inflater, container, false)
        baseBinding = binding
        return binding.root
    }

    fun setDisplayHomeAsUpEnabled(enabled: Boolean) {
        val act = activity as? AppCompatActivity ?: return
        val actionBar = act.supportActionBar ?: return
        actionBar.setDisplayHomeAsUpEnabled(enabled)

        // Mantém o logo sempre no mesmo lugar: sem a seta de voltar, reserva o espaço dela (56dp)
        act.findViewById<android.view.View>(com.wmods.wppenhacer.R.id.toolbar)?.let { tb ->
            val arrowSpace = if (enabled) 0 else (56 * tb.resources.displayMetrics.density).toInt()
            tb.setPaddingRelative(arrowSpace, tb.paddingTop, tb.paddingEnd, tb.paddingBottom)
        }
    }
}
