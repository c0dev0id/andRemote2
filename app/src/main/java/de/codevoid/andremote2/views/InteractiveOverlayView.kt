package de.codevoid.andremote2.views

interface InteractiveOverlayView {
    fun isInsideShape(localX: Float, localY: Float): Boolean
}
