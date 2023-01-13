//
//  ContentViewModel.swift
//  iosApp
//
//  Created by Robin Friedli on 13.01.23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Combine
import Foundation
import VLCUI

class ContentViewModel: ObservableObject {
    
    @Published
    var ticks: Int = 0
    @Published
    var playerState: VLCVideoPlayer.State = .opening
    @Published
    var position: Float = 0
    @Published
    var totalTicks: Int = 0
    
    let proxy: VLCVideoPlayer.Proxy = .init()
    
    var positiveTimeLabel: String {
        (ticks.roundDownNearestThousand / 1000).timeLabel
    }
    
    var negativeTimeLabel: String {
        ((totalTicks.roundDownNearestThousand - ticks.roundDownNearestThousand) / 1000).timeLabel
    }
    
    func setCustomPosition(_ newPosition: Float) {
        position = newPosition
    }
    
    func onStateUpdated(_ newState: VLCVideoPlayer.State, _ playbackInformation: VLCVideoPlayer.PlaybackInformation) {
        playerState = newState
    }
    
    func onTicksUpdated(_ newTicks: Int, _ playbackInformation: VLCVideoPlayer.PlaybackInformation) {
        position = playbackInformation.position
        ticks = newTicks
        totalTicks = playbackInformation.length
    }
}
