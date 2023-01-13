//
//  IntExtension.swift
//  iosApp
//
//  Created by Robin Friedli on 13.01.23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation

extension Int {

    var timeLabel: String {
        let formatter = DateComponentsFormatter()
        formatter.unitsStyle = .abbreviated
        if self >= 3600 {
            formatter.allowedUnits = [.hour, .minute, .second]
        } else {
            formatter.allowedUnits = [.minute, .second]
        }
        formatter.unitsStyle = .positional
        formatter.zeroFormattingBehavior = .pad

        return formatter.string(from: TimeInterval(self)) ?? "--:--"
    }
}
