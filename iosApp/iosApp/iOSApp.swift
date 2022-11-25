import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            NavigationView {
                ContentView().navigationTitle("filebroker")
            }
        }
    }
}

struct MenuView: View {
    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Image(systemName: "home")
                    .foregroundColor(.white)
                    .imageScale(.large)
                Text("Home")
                    .foregroundColor(.white)
                    .font(.headline)
            }
            .padding(.top, 100)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
