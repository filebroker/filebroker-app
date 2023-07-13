//
//  AZVideoPlayer.swift
//  AZVideoPlayer
//
//  Created by Adam Zarn on 7/4/22.
//
// source: https://github.com/adamzarn/AZVideoPlayer/blob/main/Sources/AZVideoPlayer/AZVideoPlayer.swift
//

import Foundation
import SwiftUI
import AVKit

struct NativeVideoPlayer: View {
    
    var player: AVPlayer?
    @State var willBeginFullScreenPresentation: Bool = false
    
    var body: some View {
        AZVideoPlayer(player: player,
                      willBeginFullScreenPresentationWithAnimationCoordinator: willBeginFullScreen,
                      willEndFullScreenPresentationWithAnimationCoordinator: willEndFullScreen)
    }
    
    func willBeginFullScreen(_ playerViewController: AVPlayerViewController,
                             _ coordinator: UIViewControllerTransitionCoordinator) {
        willBeginFullScreenPresentation = true
    }
    
    func willEndFullScreen(_ playerViewController: AVPlayerViewController,
                           _ coordinator: UIViewControllerTransitionCoordinator) {
        // This is a static helper method provided by AZVideoPlayer to keep
        // the video playing if it was playing when full screen presentation ended
        AZVideoPlayer.continuePlayingIfPlaying(player, coordinator)
    }
}

public struct AZVideoPlayer: UIViewControllerRepresentable {
    public typealias TransitionCompletion = (
        AVPlayerViewController, UIViewControllerTransitionCoordinator
    ) -> Void
    
    let player: AVPlayer?
    let showsPlaybackControls: Bool
    let willBeginFullScreenPresentationWithAnimationCoordinator: TransitionCompletion?
    let willEndFullScreenPresentationWithAnimationCoordinator: TransitionCompletion?
    
    public init(player: AVPlayer?,
                willBeginFullScreenPresentationWithAnimationCoordinator: TransitionCompletion? = nil,
                willEndFullScreenPresentationWithAnimationCoordinator: TransitionCompletion? = nil,
                showsPlaybackControls: Bool = true) {
        self.player = player
        self.showsPlaybackControls = showsPlaybackControls
        self.willBeginFullScreenPresentationWithAnimationCoordinator = willBeginFullScreenPresentationWithAnimationCoordinator
        self.willEndFullScreenPresentationWithAnimationCoordinator = willEndFullScreenPresentationWithAnimationCoordinator
    }
    
    public func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.showsPlaybackControls = showsPlaybackControls
        controller.delegate = context.coordinator
        controller.allowsPictureInPicturePlayback = true
        controller.canStartPictureInPictureAutomaticallyFromInline = true
        return controller
    }
    
    public func updateUIViewController(_ controller: AVPlayerViewController, context: Context) {
        controller.player = player
    }
    
    public func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    public final class Coordinator: NSObject, AVPlayerViewControllerDelegate {
        var parent: AZVideoPlayer
        
        init(_ parent: AZVideoPlayer) {
            self.parent = parent
        }
        
        public func playerViewController(_ playerViewController: AVPlayerViewController,
                                         willBeginFullScreenPresentationWithAnimationCoordinator coordinator: UIViewControllerTransitionCoordinator) {
            parent.willBeginFullScreenPresentationWithAnimationCoordinator?(playerViewController, coordinator)
        }
        
        public func playerViewController(_ playerViewController: AVPlayerViewController,
                                         willEndFullScreenPresentationWithAnimationCoordinator coordinator: UIViewControllerTransitionCoordinator) {
            parent.willEndFullScreenPresentationWithAnimationCoordinator?(playerViewController, coordinator)
        }
    }
    
    public static func continuePlayingIfPlaying(_ player: AVPlayer?,
                                                _ coordinator: UIViewControllerTransitionCoordinator) {
        let isPlaying = player?.isPlaying ?? false
        coordinator.animate(alongsideTransition: nil) { _ in
            if isPlaying {
                player?.play()
            }
        }
    }
}

extension AVPlayer {
    var isPlaying: Bool {
        return rate != 0 && error == nil
    }
}
