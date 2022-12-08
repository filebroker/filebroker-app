//
//  PostView.swift
//  iosApp
//
//  Created by Robin Friedli on 07.12.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct ProfileView: View {
    
    @ObservedObject var env: Env
    
    var body: some View {
        if let login = env.api.currentLogin {
            VStack {
                Text("Welcome, you are logged in as")
                Text(login.user.user_name)
            }
        } else {
            Text("Not logged in")
        }
    }
}
