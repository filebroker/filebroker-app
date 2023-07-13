//
//  VlcVideoPlayer.swift
//  iosApp
//
//  Created by Robin Friedli on 13.05.23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import VLCUI

struct VlcVideoPlayer: View {
    
    var vlcPlayerConfiguration: VLCVideoPlayer.Configuration
    
    @StateObject
    private var videoContentViewModel = ContentViewModel()
    @State var hideOverlay = false
    @State var overlayHideTimer: Timer? = nil
    @State var isFullScreen = false
    
    var body: some View {
        ZStack(alignment: .top) {
            let player = VLCVideoPlayer(configuration: vlcPlayerConfiguration)
                .proxy(videoContentViewModel.proxy)
                .onStateUpdated(videoContentViewModel.onStateUpdated)
                .onTicksUpdated(videoContentViewModel.onTicksUpdated)
                .onDisappear {
                    videoContentViewModel.proxy.stop()
                }
            
            if isFullScreen {
                player
                    .frame(maxHeight: .infinity)
                    .edgesIgnoringSafeArea(.all)
                    .statusBar(hidden: true)
            } else {
                player
            }
            
            VStack {
                Button {
                    isFullScreen.toggle()
                } label: {
                    Group {
                        if isFullScreen {
                            Image(systemName: "arrow.down.forward.and.arrow.up.backward")
                        } else {
                            Image(systemName: "arrow.up.backward.and.arrow.down.forward")
                        }
                    }
                    .font(.system(size: 52, weight: .heavy, design: .default))
                    .frame(maxWidth: 30)
                    .foregroundColor(.white)
                }
                Spacer()
                let overlay = OverlayView(viewModel: videoContentViewModel) { hideAfter in
                    overlayHideTimer?.invalidate()
                    if hideAfter {
                        overlayHideTimer = Timer.scheduledTimer(withTimeInterval: 3, repeats: false) { timer in
                            self.hideOverlay = true
                        }
                    }
                }
                    .padding()
                    .onAppear {
                        overlayHideTimer = Timer.scheduledTimer(withTimeInterval: 3, repeats: false) { timer in
                            self.hideOverlay = true
                        }
                    }
                
                if hideOverlay {
                    overlay.hidden()
                    // show buffering overlay even if the rest of the overlay is hidden
                    if videoContentViewModel.playerState == .buffering {
                        VStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
                    }
                } else {
                    overlay
                }
            }
        }
        .onTapGesture {
            overlayHideTimer?.invalidate()
            withAnimation {
                hideOverlay.toggle()
            }
        }
    }
}
