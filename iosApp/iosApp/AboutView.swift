//
//  AboutView.swift
//  iosApp
//
//  Created by Robin Friedli on 09.12.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import shared
import SwiftUI

struct AboutView: View {
    var body: some View {
        Text("Filebroker 0.1")
        Text("Platform: \(IOSPlatform().name)")
    }
}
