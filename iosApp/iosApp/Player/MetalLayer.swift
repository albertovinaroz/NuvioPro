import Foundation
import UIKit
import QuartzCore
import CoreVideo
import Metal

class MetalLayer: CAMetalLayer {
    weak var pictureInPictureSink: PlayerPictureInPictureController?
    private let pictureInPictureLock = NSLock()
    private var consumesVideo = false
    private var presentationGeneration: UInt = 0
    private var presentationObserver: (generation: UInt, callback: (TimeInterval) -> Void)?

    func observeNextPresentedDrawable(_ callback: @escaping (TimeInterval) -> Void) {
        pictureInPictureLock.lock()
        presentationGeneration &+= 1
        presentationObserver = (presentationGeneration, callback)
        pictureInPictureLock.unlock()
    }

    func cancelPresentationObserver() {
        pictureInPictureLock.lock()
        presentationGeneration &+= 1
        presentationObserver = nil
        pictureInPictureLock.unlock()
    }

    /// Hands [drawable] to a pending presentation observer registered before it was acquired.
    private func notifyPresentationObserver(of drawable: CAMetalDrawable, generation: UInt?) {
        guard let generation else { return }
        pictureInPictureLock.lock()
        let observer = presentationObserver?.generation == generation ? presentationObserver : nil
        if observer != nil { presentationObserver = nil }
        pictureInPictureLock.unlock()
        guard let observer else { return }
        let deliver: (TimeInterval) -> Void = { [weak self] time in
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                self.pictureInPictureLock.lock()
                let current = self.presentationGeneration == generation
                self.pictureInPictureLock.unlock()
                if current { observer.callback(time) }
            }
        }
        #if targetEnvironment(simulator)
        // The Simulator's Metal has no presented handlers; report the drawable as shown now.
        deliver(CACurrentMediaTime())
        #else
        drawable.addPresentedHandler { presented in deliver(presented.presentedTime) }
        #endif
    }

    @objc var nuvioPiPConsumesVideo: Bool {
        get {
            pictureInPictureLock.lock()
            defer { pictureInPictureLock.unlock() }
            return consumesVideo
        }
        set {
            pictureInPictureLock.lock()
            consumesVideo = newValue
            pictureInPictureLock.unlock()
        }
    }

    @objc func nuvioEnqueuePiPFrame(_ pixelBuffer: CVPixelBuffer, presentationTime: Double, isPictureInPictureFrame: Bool) {
        pictureInPictureSink?.submit(pixelBuffer, pts: presentationTime, isPictureInPictureFrame: isPictureInPictureFrame)
    }

    // Drawable acquisition can stall while iOS denies GPU work (backgrounded app, lost device):
    // after a couple of failures nextDrawable() stops hammering Metal and only probes now and then,
    // until a drawable comes back or setRenderingSuspended(false) is called.
    var onRenderingSuspensionChanged: ((Bool, String) -> Void)?

    private let suspensionLock = NSLock()

    private static let failureThresholdBeforeSuspension = 2
    private static let suspendedRetryInterval: CFTimeInterval = 1.0
    private static let suspendedIdleSleep: TimeInterval = 0.03

    private var isRenderingSuspended = false
    private var isSuspensionLatched = false
    private var consecutiveAcquisitionFailures = 0
    private var lastSuspendedProbeTime: CFTimeInterval = 0

    override var drawableSize: CGSize {
        get { return super.drawableSize }
        set {
            if Int(newValue.width) > 1 && Int(newValue.height) > 1 {
                super.drawableSize = newValue
            }
        }
    }

    override var wantsExtendedDynamicRangeContent: Bool {
        get { return super.wantsExtendedDynamicRangeContent }
        set {
            if Thread.isMainThread {
                super.wantsExtendedDynamicRangeContent = newValue
            } else {
                // mpv's vo thread sets this during video-output init while it holds the
                // core lock; a sync hop here deadlocks against main-thread property reads.
                DispatchQueue.main.async {
                    super.wantsExtendedDynamicRangeContent = newValue
                }
            }
        }
    }

    func setRenderingSuspended(_ suspended: Bool, reason: String) {
        suspensionLock.lock()
        let changed = isRenderingSuspended != suspended
        isRenderingSuspended = suspended
        isSuspensionLatched = suspended
        consecutiveAcquisitionFailures = 0
        lastSuspendedProbeTime = suspended ? CACurrentMediaTime() : 0
        suspensionLock.unlock()

        if changed { onRenderingSuspensionChanged?(suspended, reason) }
    }

    override func nextDrawable() -> CAMetalDrawable? {
        pictureInPictureLock.lock()
        let observerGeneration = presentationObserver?.generation
        pictureInPictureLock.unlock()

        suspensionLock.lock()
        if isRenderingSuspended {
            let now = CACurrentMediaTime()
            let shouldProbe = now - lastSuspendedProbeTime >= Self.suspendedRetryInterval
            if shouldProbe {
                lastSuspendedProbeTime = now
            }
            suspensionLock.unlock()

            guard shouldProbe else {
                Thread.sleep(forTimeInterval: Self.suspendedIdleSleep)
                return nil
            }
        } else {
            suspensionLock.unlock()
        }

        let drawable = super.nextDrawable()

        suspensionLock.lock()
        var didSuspend = false
        var didResume = false
        if drawable == nil {
            consecutiveAcquisitionFailures += 1
            if !isRenderingSuspended &&
                consecutiveAcquisitionFailures >= Self.failureThresholdBeforeSuspension {
                isRenderingSuspended = true
                lastSuspendedProbeTime = CACurrentMediaTime()
                didSuspend = true
            }
        } else {
            consecutiveAcquisitionFailures = 0
            if isRenderingSuspended && !isSuspensionLatched {
                isRenderingSuspended = false
                lastSuspendedProbeTime = 0
                didResume = true
            }
        }
        suspensionLock.unlock()

        if didSuspend { onRenderingSuspensionChanged?(true, "drawable-acquisition-stalled") }
        if didResume { onRenderingSuspensionChanged?(false, "drawable-acquired") }

        if let drawable { notifyPresentationObserver(of: drawable, generation: observerGeneration) }
        return drawable
    }
}
