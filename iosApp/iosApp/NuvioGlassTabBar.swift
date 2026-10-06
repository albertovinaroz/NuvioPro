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
    @State private var dragLocationX: CGFloat?

    private struct TabFramePreferenceKey: PreferenceKey {
        static var defaultValue: [NuvioAppTab: CGRect] = [:]
        static func reduce(value: inout [NuvioAppTab: CGRect], nextValue: () -> [NuvioAppTab: CGRect]) {
            value.merge(nextValue()) { _, new in new }
        }
    }

    private var dragAcrossTabsGesture: some Gesture {
        // Like the system bar: a lens follows the finger and the tab under it is only selected on
        // release, so dragging across doesn't load every screen it passes over.
        DragGesture(minimumDistance: 8, coordinateSpace: .named(Self.tabBarCoordinateSpaceName))
            .onChanged { value in
                guard isExpanded, !tabFrames.isEmpty else { return }
                dragLocationX = value.location.x
                guard let hitTab = tab(nearestTo: value.location.x), dragActiveTab != hitTab else { return }
                dragActiveTab = hitTab
                if Self.tapHapticsEnabled {
                    Self.tapFeedback.impactOccurred()
                    Self.tapFeedback.prepare()
                }
            }
            .onEnded { _ in
                if let target = dragActiveTab, target != selectedTab {
                    appCoordinator.selectedTab = target
                }
                withAnimation(.smooth(duration: 0.22)) {
                    dragActiveTab = nil
                    dragLocationX = nil
                }
            }
    }

    private func tab(nearestTo x: CGFloat) -> NuvioAppTab? {
        tabFrames.min { abs($0.value.midX - x) < abs($1.value.midX - x) }?.key
    }

    @ViewBuilder
    private var dragLens: some View {
        if let x = dragLocationX,
           let hovered = dragActiveTab,
           let frame = tabFrames[hovered] {
            let midXs = tabFrames.values.map(\.midX)
            let clampedX = min(max(x, midXs.min() ?? x), midXs.max() ?? x)
            // Its own Liquid Glass, outside the bar's GlassEffectContainer so it refracts the bar
            // and content beneath it instead of merging into the bar's shape — the same lens the
            // system tab bar shows while dragging, lightly tinted with the theme accent.
            Color.clear
                .frame(width: frame.width + 12, height: frame.height + 8)
                .glassEffect(
                    .clear
                        .tint(Color(uiColor: iconStore.accentColor).opacity(0.14))
                        .interactive(),
                    in: Capsule()
                )
                .scaleEffect(1.12)
                .position(x: clampedX, y: frame.midY)
                .allowsHitTesting(false)
            // Glass blurs whatever is under it, so the hovered tab is redrawn crisp on top of the
            // lens (as the system bar does) instead of being seen through it.
            lensContent(for: hovered)
                .scaleEffect(1.12)
                .position(x: frame.midX, y: frame.midY)
                .allowsHitTesting(false)
                .transition(.scale(scale: 0.85).combined(with: .opacity))
                .animation(.interactiveSpring(response: 0.25, dampingFraction: 0.8), value: clampedX)
        }
    }

    @ViewBuilder
    private func lensContent(for tab: NuvioAppTab) -> some View {
        if verticalSizeClass == .compact {
            HStack(spacing: 6) {
                icon(for: tab, selected: true)
                label(for: tab, selected: true)
            }
        } else {
            VStack(spacing: 3) {
                icon(for: tab, selected: true)
                label(for: tab, selected: true)
            }
        }
    }

    private var mirroredItems: [NuvioTabBarItemMetrics]? {
        guard isExpanded,
              let items = expandedMetrics?.items,
              !items.isEmpty,
              items.count == visibleTabs.count else { return nil }
        return items
    }

    // One UISegmentedControl-based bar for both shapes and both orientations: it carries the native
    // Liquid Glass lens when expanded, and its single glass view springs between bar and pill so the
    // collapse/expand is a true glass morph. The SwiftUI bar below remains only for mirrored metrics.
    private var usesNativeSegmentedBar: Bool {
        expandedMetrics == nil
    }

    var body: some View {
        Group {
            if usesNativeSegmentedBar {
                segmentedBar
                    .padding(.horizontal, 16)
            } else {
                pillBar
                    .frame(maxWidth: .infinity, alignment: isExpanded ? .center : .leading)
                    .padding(.horizontal, isExpanded ? 20 : 16)
                    .animation(.smooth(duration: 0.32), value: isExpanded)
            }
        }
        .padding(.bottom, bottomInset)
        .ignoresSafeArea(.container, edges: .bottom)
        .animation(.smooth(duration: 0.22), value: selectedTab)
    }

    private var pillBar: some View {
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
        .overlay { dragLens }
        .coordinateSpace(name: Self.tabBarCoordinateSpaceName)
        .onPreferenceChange(TabFramePreferenceKey.self) { tabFrames = $0 }
        .simultaneousGesture(dragAcrossTabsGesture)
    }

    private var isCompactHeight: Bool {
        verticalSizeClass == .compact
    }

    private var segmentedBar: some View {
        let tabs = appCoordinator.availableTabs
        let singleAccent = iconStore.accentColors.count <= 1
        let items = tabs.map { tab in
            NuvioSegmentedTabBarItem(
                title: appCoordinator.title(for: tab),
                baseImage: iconStore.image(for: tab, selected: false),
                accentImage: iconStore.image(for: tab, selected: true),
                tintsBaseImage: tab != .settings,
                tintsAccentImage: tab != .settings && singleAccent
            )
        }
        let contentKey = tabs.map { "\($0.rawValue):\(appCoordinator.title(for: $0))" }.joined(separator: "|")
            + "#\(iconStore.revision)#\(isCompactHeight)"
        return NuvioSegmentedTabBar(
            items: items,
            contentKey: contentKey,
            selectedIndex: tabs.firstIndex(of: selectedTab) ?? 0,
            isExpanded: isExpanded,
            isCompact: isCompactHeight,
            accentColor: iconStore.accentColor,
            indicatorColor: iconStore.accentColors.isEmpty
                ? iconStore.accentColor
                : iconStore.accentColors[iconStore.accentColors.count / 2],
            hapticsEnabled: { Self.tapHapticsEnabled },
            onSelect: { index in
                guard tabs.indices.contains(index) else { return }
                appCoordinator.selectedTab = tabs[index]
            },
            onReselect: { index in
                guard tabs.indices.contains(index) else { return }
                // Matches the system tab bar's reselect convention (scroll to top).
                NativeTabBridgeKt.nativeTabSelect(tabName: tabs[index].rawValue)
            },
            onLongPress: { index in
                guard tabs.indices.contains(index), tabs[index] == .settings, appCoordinator.isAppReady else { return }
                appCoordinator.isProfileSwitcherPresented = true
            },
            onCollapsedTap: {
                if Self.tapHapticsEnabled {
                    Self.tapFeedback.impactOccurred()
                    Self.tapFeedback.prepare()
                }
                appCoordinator.requestTabBarVisible(true)
            }
        )
        .frame(height: NuvioSegmentedTabBar.height(compact: isCompactHeight))
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
            if selected && isExpanded && dragLocationX == nil {
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
