//
//  OverlayView.swift
//  iosApp
//
//  Created by Robin Friedli on 13.01.23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation

import SwiftUI
import VLCUI

struct OverlayView: View {
    
    @ObservedObject
    var viewModel: ContentViewModel
    @State
    var isScrubbing: Bool = false
    @State
    var currentPosition: Float = 0
    
    var onInteractCb: ((Bool) -> Void)?
    
    var body: some View {
        VStack {
            Spacer()
            HStack(alignment: .center, spacing: 60) {
                Button {
                    onInteractCb?.self(true)
                    viewModel.proxy.jumpBackward(15)
                } label: {
                    Image(systemName: "gobackward.15")
                        .font(.system(size: 32, weight: .regular, design: .default))
                        .foregroundColor(.white)
                }
                
                Button {
                    onInteractCb?.self(true)
                    if viewModel.playerState == .playing {
                        viewModel.proxy.pause()
                    } else {
                        viewModel.proxy.play()
                    }
                } label: {
                    Group {
                        if viewModel.playerState == .playing {
                            Image(systemName: "pause.fill")
                        } else if viewModel.playerState == .buffering {
                            ProgressView()
                        } else {
                            Image(systemName: "play.fill")
                        }
                    }
                    .font(.system(size: 52, weight: .heavy, design: .default))
                    .frame(maxWidth: 30)
                    .foregroundColor(.white)
                }
                
                Button {
                    onInteractCb?.self(true)
                    viewModel.proxy.jumpForward(15)
                } label: {
                    Image(systemName: "goforward.15")
                        .font(.system(size: 32, weight: .light, design: .default))
                        .foregroundColor(.white)
                }
            }
            Spacer()
            HStack(spacing: 5) {
                Text(viewModel.positiveTimeLabel)
                    .frame(width: 75)
                
                Slider(
                    value: $currentPosition,
                    in: 0 ... Float(1.0)
                ) { isEditing in
                    onInteractCb?.self(!isEditing)
                    isScrubbing = isEditing
                }
                
                Text(viewModel.negativeTimeLabel)
                    .frame(width: 75)
            }
            .onChange(of: isScrubbing) { isScrubbing in
                guard !isScrubbing else { return }
                viewModel.proxy.setTime(.ticks(viewModel.totalTicks * Int(currentPosition * 100) / 100))
            }
            .onChange(of: viewModel.position) { newValue in
                guard !isScrubbing else { return }
                currentPosition = newValue
            }
        }
    }
}
