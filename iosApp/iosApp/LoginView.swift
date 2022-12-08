//
//  LoginView.swift
//  iosApp
//
//  Created by Robin Friedli on 06.12.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import os
import SwiftUI
import shared

struct LoginView: View {
    
    @State private var userName = ""
    @State private var password = ""
    
    @State var isError = false
    @State var errorCode: String? = nil
    
    @ObservedObject var env: Env
    
    @Environment(\.presentationMode) var presentationMode: Binding<PresentationMode>
    
    var body: some View {
        VStack {
            TextField("User Name", text: $userName)
                .autocapitalization(.none)
                .disableAutocorrection(true)
                .padding()
            SecureField("Password", text: $password)
                .autocapitalization(.none)
                .disableAutocorrection(true)
                .padding()
            Button("Sign In", action: {
                env.api.login(request: Api.LoginRequest(user_name: userName, password: password)) { loginResponse, error in
                    DispatchQueue.main.async {
                        if let error = error {
                            self.isError = true
                            let kotlinError = (error as NSError).kotlinException
                            if kotlinError is Api.InvalidCredentialsException {
                                self.errorCode = "Invalid Credentials"
                            } else if kotlinError is Api.InvalidHttpResponseException {
                                let status = (kotlinError as! Api.InvalidHttpResponseException).status
                                Logger().error("Login failed with status \(status): \(error.localizedDescription)")
                                self.errorCode = "Request failed with status " + status.formatted()
                            } else {
                                Logger().error("Login failed with unexpected error: \(error.localizedDescription)")
                                self.errorCode = error.localizedDescription
                            }
                        } else {
                            self.presentationMode.wrappedValue.dismiss()
                        }
                    }
                }
            })
            .alert("Error", isPresented: $isError, actions: {}, message: {Text(self.errorCode ?? "")})
        }
        .navigationBarTitle("Login", displayMode: .inline)
    }
}
