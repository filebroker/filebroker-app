import SwiftUI
import shared

struct ContentView: View {
    
    @State var query = ""
    
    @ObservedObject var env: Env
    
    var body: some View {
        NavigationStack {
            VStack {
                NavigationLink(destination: PostsView(env: env, query: query)) {
                    Text("Search")
                }
                Spacer()
                List {
                    NavigationLink(destination: PostsView(env: env, query: query)) {
                        HStack {
                            Image(systemName: "folder")
                                .foregroundColor(.gray)
                                .imageScale(.large)
                                .frame(width: 25)
                            Text("Posts")
                                .foregroundColor(.gray)
                                .font(.headline)
                        }
                    }
                    if env.api.currentLogin == nil {
                        NavigationLink(destination: LoginView(env: env)) {
                            HStack {
                                Image(systemName: "person")
                                    .foregroundColor(.gray)
                                    .imageScale(.large)
                                    .frame(width: 25)
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
                                    .frame(width: 25)
                                Text(env.api.currentLogin?.user.user_name ?? "Profile")
                                    .foregroundColor(.gray)
                                    .font(.headline)
                            }
                        }
                    }
                    NavigationLink(destination: AboutView()) {
                        HStack {
                            Image(systemName: "info.circle")
                                .foregroundColor(.gray)
                                .imageScale(.large)
                                .frame(width: 25)
                            Text("About")
                                .foregroundColor(.gray)
                                .font(.headline)
                        }
                    }
                }
                .listStyle(.insetGrouped)
            }
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search Posts")
            .disableAutocorrection(true)
            .autocapitalization(.none)
            .navigationBarTitle("filebroker", displayMode: .large)
        }
    }
}
