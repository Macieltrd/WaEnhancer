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
        // Sem seta de voltar em nenhuma tela: o logo fica sempre no mesmo lugar,
        // alinhado com a borda esquerda dos cartões (16dp). Voltar = botão/gesto do Android.
        actionBar.setDisplayHomeAsUpEnabled(false)

        act.findViewById<android.view.View>(com.wmods.wppenhacer.R.id.toolbar)?.let { tb ->
            val d = tb.resources.displayMetrics.density
            val margin = (16 * d).toInt()   // logo alinhado com a borda esquerda dos cartões
            val endFix = (2 * d).toInt()    // ícones da direita alinhados com a borda direita dos cartões
            tb.setPaddingRelative(margin, tb.paddingTop, endFix, tb.paddingBottom)
        }
    }
}
