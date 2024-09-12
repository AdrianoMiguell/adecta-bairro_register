package com.miguelprojects.myapplication.adapter.decoration

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MarginItemDecoration(
    private val context: Context,
    private val marginTop: Int,
    private val marginBottom: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0

        // Aplica margem apenas ao primeiro item
        if (position == 0) {
            outRect.top = marginTop
        }

        // Aplica margem apenas ao último item
        if (position == itemCount - 1) {
            outRect.bottom = marginBottom
        }
    }
}
