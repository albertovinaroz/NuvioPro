import SwiftUI
import UIKit
import ComposeApp

/// The Compose floating tab bar ("off" behavior, and iOS before 26) drawn once above every tab's
/// navigation stack, instead of a copy inside each screen that slid away on every settings push.
///
/// The Compose canvas is transparent; haze can't sample the other Compose scenes beneath it, so a
/// native blur sits behind the canvas, kept on the bar's glass as Compose reports where it is.
@available(iOS 16.0, *)
struct FloatingTabBarOverlayView: UIViewControllerRepresentable {
    let appCoordinator: AppNavigationCoordinator
    let usesTabletFloatingTabBar: Bool

    func makeUIViewController(context: Context) -> FloatingTabBarHostController {
        let host = FloatingTabBarHostController()
        let coordinator = appCoordinator
        let compose = MainViewControllerKt.FloatingTabBarViewController(
            appGateController: coordinator.appGateController,
            useTabletFloatingTabBar: usesTabletFloatingTabBar,
            onSelectTab: { tabName in
                guard let tab = NuvioAppTab.from(kotlinName: tabName) else { return }
                if tab == coordinator.selectedTab {
                    let tabCoordinator = coordinator.coordinator(for: tab)
                    if tabCoordinator.path.isEmpty {
                        // Reselect: the tab's own screen scrolls to top (or its root action).
                        NativeTabBridgeKt.nativeTabSelect(tabName: tab.rawValue)
                    } else {
                        tabCoordinator.popToRoot()
                    }
                } else {
                    coordinator.activateTab(named: tabName)
                }
            },
            onGlassBounds: { [weak host] x, y, width, height in
                host?.updateGlass(
                    CGRect(
                        x: CGFloat(x.floatValue),
                        y: CGFloat(y.floatValue),
                        width: CGFloat(width.floatValue),
                        height: CGFloat(height.floatValue)
                    )
                )
            },
            onCapturesAllTouches: { [weak host] captures in
                host?.capturesAllTouches = captures.boolValue
            }
        )
        host.install(compose)
        return host
    }

    func updateUIViewController(_ uiViewController: FloatingTabBarHostController, context: Context) {}
}

final class FloatingTabBarHostController: UIViewController {
    private let blurView = UIVisualEffectView(effect: UIBlurEffect(style: .systemUltraThinMaterialDark))
    /// Where the bar's glass is, in this view's coordinates; touches elsewhere pass through.
    fileprivate private(set) var glassFrame: CGRect = .zero
    /// Set while something bigger than the bar is up (the profile switcher's popup).
    var capturesAllTouches = false

    override func loadView() {
        let view = FloatingTabBarPassThroughView()
        view.owner = self
        view.backgroundColor = .clear
        view.isOpaque = false
        self.view = view
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        blurView.isUserInteractionEnabled = false
        blurView.clipsToBounds = true
        blurView.isHidden = true
        view.addSubview(blurView)
    }

    func install(_ compose: UIViewController) {
        loadViewIfNeeded()
        addChild(compose)
        compose.view.frame = view.bounds
        compose.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        compose.view.backgroundColor = .clear
        compose.view.isOpaque = false
        view.addSubview(compose.view)
        compose.didMove(toParent: self)
    }

    func updateGlass(_ frame: CGRect) {
        // Called from Compose's draw pass, every frame the bar moves: no implicit animations, so
        // the blur lands exactly where the glass is drawn this frame.
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        if frame.width <= 0 || frame.height <= 0 {
            blurView.isHidden = true
            glassFrame = .zero
        } else {
            blurView.frame = frame
            blurView.layer.cornerRadius = min(frame.width, frame.height) / 2
            blurView.isHidden = false
            glassFrame = frame
        }
        CATransaction.commit()
    }
}

private final class FloatingTabBarPassThroughView: UIView {
    weak var owner: FloatingTabBarHostController?

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        guard let owner else { return false }
        if owner.capturesAllTouches { return true }
        // A little slack around the glass, for the jelly stretch and near-miss taps on its edge.
        return owner.glassFrame.insetBy(dx: -6, dy: -12).contains(point)
    }
}
