package com.nuvio.app.core.ui

import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * Finds the view controller that should present a modal (a document picker, share sheet, etc.)
 * right now.
 *
 * `UIApplication.keyWindow` is deprecated and unreliable on iPad: Stage Manager and Split View
 * mean the app can own several windows/scenes at once, so it can return null (or the wrong
 * window) and any `presentViewController` call on top of it silently does nothing — from the
 * user's perspective the picker just never opens. Instead, walk the actually-foregrounded scene's
 * own windows for the one marked key, falling back to `keyWindow` only if that comes up empty
 * (e.g. an older runtime quirk).
 */
@Suppress("DEPRECATION")
internal fun topmostPresentingViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>()
    val foregroundScene = scenes.firstOrNull {
        it.activationState == UISceneActivationStateForegroundActive
    } ?: scenes.firstOrNull()

    val keyWindow: UIWindow? = foregroundScene?.windows
        ?.filterIsInstance<UIWindow>()
        ?.firstOrNull { it.isKeyWindow() }
        ?: UIApplication.sharedApplication.keyWindow

    var controller = keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
