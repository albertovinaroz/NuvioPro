import SwiftUI
import UIKit
import ComposeApp

enum NuvioTabBarBehavior: String, CaseIterable {
    case off
    case `static`
    case autoHide = "auto_hide"
    case morphed

    static let storageKey = "NuvioNativeTabBarBehavior"
    static let fallback: NuvioTabBarBehavior = .morphed

    static func current() -> NuvioTabBarBehavior {
        guard let raw = UserDefaults.standard.string(forKey: storageKey) else { return fallback }
        return NuvioTabBarBehavior(rawValue: raw) ?? fallback
    }

    var isEnabled: Bool { self != .off }

    var usesCompactPill: Bool { self == .morphed }

    var respondsToScroll: Bool { self == .autoHide || self == .morphed }
}

struct NuvioTabBarItemMetrics: Equatable {
    var buttonFrame: CGRect
    var iconFrame: CGRect
    var labelFrame: CGRect?
    var labelFont: UIFont?
}

struct NuvioTabBarMetrics: Equatable {
    var windowSize: CGSize
    var leadingInset: CGFloat
    var trailingInset: CGFloat
    var bottomInset: CGFloat
    var height: CGFloat
    var items: [NuvioTabBarItemMetrics] = []
    var selectionFrame: CGRect? = nil
    var selectionItemIndex: Int? = nil

    func selectionFrame(forItemAt index: Int) -> CGRect? {
        guard let selectionFrame,
              let measuredIndex = selectionItemIndex,
              items.indices.contains(measuredIndex),
              items.indices.contains(index) else { return nil }
        let dx = items[index].buttonFrame.midX - items[measuredIndex].buttonFrame.midX
        return selectionFrame.offsetBy(dx: dx, dy: 0)
    }

    var width: CGFloat { windowSize.width - leadingInset - trailingInset }

    static func key(for size: CGSize) -> String {
        "\(Int(size.width.rounded()))x\(Int(size.height.rounded()))"
    }
}

@available(iOS 26.0, *)
struct NuvioGlassTabBar: View {
    @ObservedObject var appCoordinator: AppNavigationCoordinator
    @ObservedObject var iconStore: NativeTabIconStore
    var expandedMetrics: NuvioTabBarMetrics? = nil

    @Namespace private var glassNamespace
    @Environment(\.verticalSizeClass) private var verticalSizeClass

    private static let barGlassID = "nuvio.tabbar"
    // Kept as one prepared instance rather than a fresh generator per tap, so the very first tap
    // after the bar appears doesn't eat the ~100ms warm-up latency.
    private static let tapFeedback: UIImpactFeedbackGenerator = {
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.prepare()
        return generator
    }()
    // Shared with HapticsSettingsStorage.ios.kt (Settings > Advanced > Tab Bar Haptics), which
    // writes this exact key — read fresh on every tap rather than cached, since the toggle can
    // flip while this bar is already on screen.
    private static var tapHapticsEnabled: Bool {
        let key = "NuvioTabBarHapticsEnabled"
        let defaults = UserDefaults.standard
        return defaults.object(forKey: key) == nil ? true : defaults.bool(forKey: key)
    }

    static let portraitBottomInset: CGFloat = 20
    static let landscapeBottomInset: CGFloat = 16

    private var bottomInset: CGFloat {
        if isExpanded, let expandedMetrics { return expandedMetrics.bottomInset }
        return verticalSizeClass == .compact ? Self.landscapeBottomInset : Self.portraitBottomInset
    }

    private var expandedHeight: CGFloat? {
        isExpanded ? expandedMetrics?.height : nil
    }

    private var selectedTab: NuvioAppTab {
        appCoordinator.selectedTab
    }

    private var isExpanded: Bool {
        appCoordinator.isTabBarVisible
    }

    private var visibleTabs: [NuvioAppTab] {
        isExpanded ? appCoordinator.availableTabs : [selectedTab]
    }

    // Real UITabBar-hosted content gets drag-across-tabs for free from UIKit; this custom pill
    // (the only tab bar instrument `morphed` shows — see the real bar staying hidden below) never
    // had that, since it was built from a row of plain Buttons that only ever recognize taps. This
    // reproduces it by hand: track each item's frame, and while the drag gesture below is active,
    // whichever tab the finger is currently over becomes selected — matching the system bar's own
    // feel, including a haptic tick on every tab it crosses into.
    private static let tabBarCoordinateSpaceName = "nuvio.tabbar.row"
    @State private var tabFrames: [NuvioAppTab: CGRect] = [:]
    @State private var dragActiveTab: NuvioAppTab?

    private struct TabFramePreferenceKey: PreferenceKey {
        static var defaultValue: [NuvioAppTab: CGRect] = [:]
        static func reduce(value: inout [NuvioAppTab: CGRect], nextValue: () -> [NuvioAppTab: CGRect]) {
            value.merge(nextValue()) { _, new in new }
        }
    }

    private var dragAcrossTabsGesture: some Gesture {
        DragGesture(minimumDistance: 8, coordinateSpace: .named(Self.tabBarCoordinateSpaceName))
            .onChanged { value in
                guard isExpanded else { return }
                guard let hitTab = tabFrames.first(where: { $0.value.contains(value.location) })?.key,
                      hitTab != selectedTab else { return }
                if dragActiveTab != hitTab {
                    dragActiveTab = hitTab
                    if Self.tapHapticsEnabled {
                        Self.tapFeedback.impactOccurred()
                        Self.tapFeedback.prepare()
                    }
                    appCoordinator.selectedTab = hitTab
                }
            }
            .onEnded { _ in
                dragActiveTab = nil
            }
    }

    private var mirroredItems: [NuvioTabBarItemMetrics]? {
        guard isExpanded,
              let items = expandedMetrics?.items,
              !items.isEmpty,
              items.count == visibleTabs.count else { return nil }
        return items
    }

    var body: some View {
        GlassEffectContainer(spacing: 0) {
            Group {
                if let mirroredItems, let expandedMetrics {
                    mirroredContent(items: mirroredItems, metrics: expandedMetrics)
                } else {
                    HStack(spacing: 0) {
                        ForEach(visibleTabs, id: \.self) { tab in
                            item(for: tab)
                                .background(
                                    GeometryReader { geometry in
                                        Color.clear.preference(
                                            key: TabFramePreferenceKey.self,
                                            value: [tab: geometry.frame(in: .named(Self.tabBarCoordinateSpaceName))]
                                        )
                                    }
                                )
                                .transition(.opacity)
                        }
                    }
                    .padding(.horizontal, 6)
                    .padding(.vertical, expandedHeight != nil ? 0 : (isExpanded ? 3 : 5))
                    .frame(height: expandedHeight)
                }
            }
            .glassEffect(.regular.interactive(), in: Capsule())
            .glassEffectID(Self.barGlassID, in: glassNamespace)
        }
        .coordinateSpace(name: Self.tabBarCoordinateSpaceName)
        .onPreferenceChange(TabFramePreferenceKey.self) { tabFrames = $0 }
        .simultaneousGesture(dragAcrossTabsGesture)
        .frame(maxWidth: .infinity, alignment: isExpanded ? .center : .leading)
        .padding(.horizontal, isExpanded ? 20 : 16)
        .padding(.bottom, bottomInset)
        .ignoresSafeArea(.container, edges: .bottom)
        // This pill is the only tab bar instrument in `morphed` — the real one stays hidden — so
        // it must stay tappable/accessible in both its expanded and collapsed shapes.
        .animation(.smooth(duration: 0.32), value: isExpanded)
        .animation(.smooth(duration: 0.22), value: selectedTab)
    }

    private func item(for tab: NuvioAppTab) -> some View {
        let selected = tab == selectedTab
        let content = Group {
            if verticalSizeClass == .compact {
                HStack(spacing: 6) {
                    icon(for: tab, selected: selected)
                    if isExpanded {
                        label(for: tab, selected: selected)
                    }
                }
            } else {
                VStack(spacing: 3) {
                    icon(for: tab, selected: selected)
                    if isExpanded {
                        label(for: tab, selected: selected)
                    }
                }
            }
        }
        .padding(.vertical, verticalSizeClass == .compact ? 6 : 7)
        .padding(.horizontal, verticalSizeClass == .compact ? 12 : (isExpanded ? 4 : 10))
        .frame(maxWidth: isExpanded && verticalSizeClass != .compact ? .infinity : nil)
        .contentShape(Capsule())
        .background {
            if selected && isExpanded {
                Capsule()
                    .fill(iconStore.accentStyle(opacity: 0.12))
            }
        }

        let button = Button {
            if Self.tapHapticsEnabled {
                Self.tapFeedback.impactOccurred()
                Self.tapFeedback.prepare()
            }
            if selected {
                if isExpanded {
                    // Tapping the already-selected tab while expanded matches the real system tab
                    // bar's convention (scroll-to-top) instead of doing nothing.
                    NativeTabBridgeKt.nativeTabSelect(tabName: tab.rawValue)
                } else {
                    appCoordinator.requestTabBarVisible(true)
                }
            } else {
                appCoordinator.selectedTab = tab
            }
        } label: {
            content
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(appCoordinator.title(for: tab)))
        .accessibilityAddTraits(selected ? [.isSelected] : [])

        return Group {
            if tab == .settings {
                button.simultaneousGesture(
                    LongPressGesture(minimumDuration: 0.45)
                        .onEnded { _ in
                            guard appCoordinator.isAppReady else { return }
                            appCoordinator.isProfileSwitcherPresented = true
                        }
                )
            } else {
                button
            }
        }
    }

    private func mirroredContent(
        items: [NuvioTabBarItemMetrics],
        metrics: NuvioTabBarMetrics
    ) -> some View {
        let selectedIndex = visibleTabs.firstIndex(of: selectedTab)
        return ZStack {
            if let selectedIndex, let selection = metrics.selectionFrame(forItemAt: selectedIndex) {
                Capsule()
                    .fill(Color.black.opacity(0.42))
                    .frame(width: selection.width, height: selection.height)
                    .position(x: selection.midX, y: selection.midY)
            }
            ForEach(Array(visibleTabs.enumerated()), id: \.element) { entry in
                mirroredItem(tab: entry.element, item: items[entry.offset])
            }
        }
        .frame(width: metrics.width, height: metrics.height)
    }

    @ViewBuilder
    private func mirroredItem(tab: NuvioAppTab, item: NuvioTabBarItemMetrics) -> some View {
        let selected = tab == selectedTab
        nativeIcon(for: tab, selected: selected)
            .frame(width: item.iconFrame.width, height: item.iconFrame.height)
            .position(x: item.iconFrame.midX, y: item.iconFrame.midY)
            .transition(.opacity)
        if let labelFrame = item.labelFrame {
            Text(appCoordinator.title(for: tab))
                .font(item.labelFont.map { Font($0 as CTFont) } ?? .system(size: 10, weight: .semibold))
                .lineLimit(1)
                .fixedSize()
                .foregroundStyle(selected ? iconStore.accentStyle() : AnyShapeStyle(Color.white))
                .position(x: labelFrame.midX, y: labelFrame.midY)
                .transition(.opacity)
        }
    }

    private func nativeIcon(for tab: NuvioAppTab, selected: Bool) -> some View {
        let image = Image(uiImage: iconStore.image(for: tab, selected: selected))
        return Group {
            if tab == .settings {
                image.renderingMode(.original).resizable().scaledToFit()
            } else if selected {
                image.renderingMode(.template).resizable().scaledToFit()
                    .foregroundStyle(iconStore.accentStyle())
            } else {
                image.renderingMode(.template).resizable().scaledToFit()
                    .foregroundStyle(Color.white)
            }
        }
    }

    private func label(for tab: NuvioAppTab, selected: Bool) -> some View {
        Text(appCoordinator.title(for: tab))
            .font(.system(size: 11, weight: .medium))
            .lineLimit(1)
            .minimumScaleFactor(0.75)
            .foregroundStyle(
                selected ? iconStore.accentStyle() : AnyShapeStyle(Color.white)
            )
            .legibleOverGlass(enabled: !selected)
    }

    private func icon(for tab: NuvioAppTab, selected: Bool) -> some View {
        let image = Image(uiImage: iconStore.image(for: tab, selected: selected))

        return Group {
            if tab == .settings {
                image
                    .renderingMode(.original)
                    .resizable()
                    .scaledToFit()
            } else if selected {
                image
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(iconStore.accentStyle())
            } else {
                image
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(Color.white)
            }
        }
        .frame(width: 24, height: 24)
        .legibleOverGlass(enabled: !selected)
    }
}

@available(iOS 26.0, *)
private extension View {
    func legibleOverGlass(enabled: Bool) -> some View {
        shadow(color: .black.opacity(enabled ? 0.35 : 0), radius: 2, x: 0, y: 0)
            .shadow(color: .black.opacity(enabled ? 0.22 : 0), radius: 5, x: 0, y: 1)
    }
}
