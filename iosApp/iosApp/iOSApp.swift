import os
import AVFAudio
import SwiftUI
import shared

@main
struct iOSApp: App {
    
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
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

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        let audioSession = AVAudioSession.sharedInstance()
        do {
          try audioSession.setCategory(.playback, mode: .moviePlayback)
        } catch {
          print("Failed to set audioSession category to playback")
        }
        return true
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
