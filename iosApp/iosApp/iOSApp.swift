import SwiftUI
import shared

@main
struct iOSApp: App {
    
    @StateObject var env = Env()
    
    var body: some Scene {
        WindowGroup {
            ContentView(env: env)
        }
    }
}

class Env : ObservableObject {
    
    @Published var currentLogin: Api.Login? = nil
    @Published var api: Api = Api()
    
    init() {
        api.loginChangeCallback = { login in
            DispatchQueue.main.async {
                self.currentLogin = login
            }
        }
    }
    
}
