import SwiftUI
import shared

struct ContentView: View {
    
    @State var query = ""
    
    @ObservedObject var env: Env
    
    var body: some View {
        NavigationStack {
            VStack {
                List {
                    if env.currentLogin == nil {
                        NavigationLink(destination: LoginView(env: env)) {
                            HStack {
                                Image(systemName: "person")
                                    .foregroundColor(.gray)
                                    .imageScale(.large)
                                Text("Login")
                                    .foregroundColor(.gray)
                                    .font(.headline)
                            }
                        }
                    } else {
                        NavigationLink(destination: ProfileView(env: env)) {
                            HStack {
                                Image(systemName: "person")
                                    .foregroundColor(.gray)
                                    .imageScale(.large)
                                Text(env.currentLogin?.user.user_name ?? "Profile")
                                    .foregroundColor(.gray)
                                    .font(.headline)
                            }
                        }
                    }
                }
                .listStyle(.insetGrouped)
            }
            .searchable(text: $query, prompt: "Search Posts")
            .navigationBarTitle("filebroker", displayMode: .large)
        }
    }
}
