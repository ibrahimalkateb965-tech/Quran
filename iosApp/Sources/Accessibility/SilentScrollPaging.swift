//
//  SilentScrollPaging.swift
//  QuranBlind
//
//  VoiceOver three-finger vertical swipe on list sheets WITHOUT the spoken scroll status.
//
//  When VoiceOver scrolls a SwiftUI ScrollView itself, it announces "Page 1 of 3" (in the
//  English system voice). The app's name-only policy forbids that. VoiceOver first offers the
//  scroll to the FOCUSED element; when that row handles it, VoiceOver never reaches the scroll
//  view and speaks no page status. Each row therefore handles the scroll: it pages the enclosing
//  UIScrollView itself and moves focus to the first row now on screen, so VoiceOver speaks only
//  that row's Arabic label.
//
//  Usage:
//      @StateObject private var pager = SilentScrollPager()
//      ScrollView { VStack { rows… .silentPageScroll(pager) } .silentPagingContainer(pager) }
//

import SwiftUI
import UIKit

/// Holds the UIScrollView behind a SwiftUI ScrollView and pages it silently.
public final class SilentScrollPager: ObservableObject {
    fileprivate weak var scrollView: UIScrollView?

    public init() {}

    /// SwiftUI reports the edge to scroll toward (UIKit's `.up` / `.down`). VoiceOver:
    /// three-finger swipe DOWN = scroll up one page (edge `.top`, reveals the rows above);
    /// three-finger swipe UP = scroll down one page (edge `.bottom`, reveals the rows below).
    /// Verified on device: the reversed mapping made every swipe hit the list's end.
    fileprivate func scroll(toward edge: Edge) {
        guard let scrollView else { return }
        switch edge {
        case .top: page(scrollView, forward: false)
        case .bottom: page(scrollView, forward: true)
        default: break
        }
    }

    private func page(_ scrollView: UIScrollView, forward: Bool) {
        let inset = scrollView.adjustedContentInset
        let visibleHeight = scrollView.bounds.height - inset.top - inset.bottom
        guard visibleHeight > 0 else { return }
        let minY = -inset.top
        let maxY = max(minY, scrollView.contentSize.height + inset.bottom - scrollView.bounds.height)
        let step = max(visibleHeight - 64, visibleHeight * 0.8)

        let current = scrollView.contentOffset.y
        let target = forward ? min(maxY, current + step) : max(minY, current - step)
        // At either end: do nothing and stay silent.
        guard abs(target - current) > 0.5 else { return }

        scrollView.setContentOffset(CGPoint(x: scrollView.contentOffset.x, y: target), animated: false)
        // Let SwiftUI lay out (and, for lazy stacks, create) the newly visible rows first.
        DispatchQueue.main.async {
            scrollView.layoutIfNeeded()
            if let row = Self.firstVisibleRow(in: scrollView) {
                UIAccessibility.post(notification: .layoutChanged, argument: row)
            }
        }
    }

    private static func firstVisibleRow(in scrollView: UIScrollView) -> NSObject? {
        let visible = scrollView.bounds.inset(by: scrollView.adjustedContentInset)
        let visibleOnScreen = UIAccessibility.convertToScreenCoordinates(visible, in: scrollView)
        var elements: [NSObject] = []
        collect(scrollView, depth: 0, into: &elements)
        return elements
            .filter { element in
                // Skip the system scroll bar ("Vertical scroll bar…") and anything unlabelled.
                guard !NSStringFromClass(type(of: element)).contains("ScrollIndicator"),
                      !(element.accessibilityLabel ?? "").isEmpty else { return false }
                let frame = element.accessibilityFrame
                return frame.height > 0
                    && frame.minY >= visibleOnScreen.minY - 1
                    && frame.minY < visibleOnScreen.maxY
            }
            .min { $0.accessibilityFrame.minY < $1.accessibilityFrame.minY }
    }

    private static func collect(_ object: NSObject, depth: Int, into result: inout [NSObject]) {
        guard depth < 60 else { return }
        if let view = object as? UIView, view.isHidden || view.accessibilityElementsHidden { return }
        if object.accessibilityElementsHidden { return }
        if object.isAccessibilityElement {
            result.append(object)
            return
        }
        let children: [NSObject]
        if let elements = object.accessibilityElements as? [NSObject], !elements.isEmpty {
            children = elements
        } else if case let count = object.accessibilityElementCount(), count != NSNotFound, count > 0 {
            children = (0..<count).compactMap { object.accessibilityElement(at: $0) as? NSObject }
        } else if let view = object as? UIView {
            children = view.subviews
        } else {
            children = []
        }
        children.forEach { collect($0, depth: depth + 1, into: &result) }
    }
}

public extension View {
    /// Apply to the content inside the vertical `ScrollView`; captures its UIScrollView.
    func silentPagingContainer(_ pager: SilentScrollPager) -> some View {
        background(
            ScrollViewProbe(pager: pager)
                .frame(width: 0, height: 0)
                .accessibilityHidden(true)
        )
    }

    /// Apply to every row: VoiceOver's three-finger vertical swipe pages the list silently.
    func silentPageScroll(_ pager: SilentScrollPager) -> some View {
        accessibilityScrollAction { edge in
            pager.scroll(toward: edge)
        }
    }
}

/// Zero-size view inside the scroll content that hands the enclosing UIScrollView to the pager.
private struct ScrollViewProbe: UIViewRepresentable {
    let pager: SilentScrollPager

    func makeUIView(context: Context) -> ProbeView {
        let view = ProbeView()
        view.pager = pager
        return view
    }

    func updateUIView(_ uiView: ProbeView, context: Context) {
        uiView.pager = pager
        uiView.attach()
    }

    final class ProbeView: UIView {
        weak var pager: SilentScrollPager?

        override init(frame: CGRect) {
            super.init(frame: frame)
            isUserInteractionEnabled = false
            isAccessibilityElement = false
            accessibilityElementsHidden = true
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            attach()
        }

        func attach() {
            var view = superview
            while let current = view {
                if let scrollView = current as? UIScrollView {
                    pager?.scrollView = scrollView
                    return
                }
                view = current.superview
            }
        }
    }
}
