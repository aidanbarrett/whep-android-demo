package com.example.whepexample

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView


class MediaWebView : WebView {
    constructor(context: Context) : super(context!!)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onWindowVisibilityChanged(visibility: Int) {
        if (visibility != GONE) super.onWindowVisibilityChanged(VISIBLE)
    }
}