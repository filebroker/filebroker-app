import os
import SwiftUI
import shared

@main
struct iOSApp: App {
    
    @StateObject var env = Env()
    
    var body: some Scene {
        WindowGroup {
            ContentView(env: env)
                .onAppear {
                    let refreshToken = UserDefaults().string(forKey: "refresh_token")
                    if refreshToken != nil {
                        env.api.refreshLogin(refreshToken: refreshToken!) { login, error in
                            if let error = error {
                                Logger().error("Failed to refresh login with error \(error.localizedDescription)")
                            }
                        }
                    }
                }
        }
    }
}

class Env : ObservableObject {

    @Published var api: Api = Api()
    
    init() {
        api.loginChangeCallback = { login in
            DispatchQueue.main.async {
                self.objectWillChange.send()
                if login != nil {
                    UserDefaults().set(login!.refreshToken, forKey: "refresh_token")
                } else {
                    UserDefaults().removeObject(forKey: "refresh_token")
                }
            }
        }
    }
    
}
