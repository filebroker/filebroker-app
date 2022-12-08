//
//  PostsView.swift
//  iosApp
//
//  Created by Robin Friedli on 07.12.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import os
import shared

struct PostsView: View {
    
    @ObservedObject var env: Env
    
    @State var query: String
    @State var currentQuery = ""
    @State var currentPage: Int64 = 0
    
    @State private var gridColumns = Array(repeating: GridItem(.flexible()), count: 3)
    @State var posts: [Api.PostQueryObject] = []
    @State var fullCount: Int64? = nil
    @State var pageCount: Int64? = nil
    
    @State var isError = false
    @State var errorCode: String? = nil
    
    var body: some View {
        SearchView { isSearching in
            VStack {
                HStack {
                    if fullCount != nil {
                        Text("\(fullCount!) Results")
                    }
                    if fullCount != nil && pageCount != nil {
                        Spacer()
                    }
                    if pageCount != nil {
                        Text("\(currentPage + 1) / \(pageCount!)")
                    }
                }
                ScrollView {
                    LazyVGrid(columns: gridColumns) {
                        ForEach(posts) { post in
                            GeometryReader { geometry in
                                NavigationLink(destination: PostDetailView(env: env, postKey: post.pk, query: query, currentPage: currentPage)) {
                                    GridItemView(post: post, width: geometry.size.width)
                                }
                            }
                            .cornerRadius(8.0)
                            .aspectRatio(320 / 180, contentMode: .fit)
                        }
                    }
                }
            }.onChange(of: isSearching) { newVal in
                if !newVal {
                    DispatchQueue.main.async {
                        query = currentQuery
                    }
                }
            }
        }
        .onAppear {
            currentQuery = query
            executeSearch()
        }
        .navigationBarTitle("Posts", displayMode: .inline)
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always))
        .disableAutocorrection(true)
        .autocapitalization(.none)
        .alert("Error", isPresented: $isError, actions: {}, message: {Text(self.errorCode ?? "")})
        .onSubmit(of: .search) {
            currentQuery = query
            executeSearch()
        }
        .navigationBarItems(trailing: HStack {
            let firstPage = currentPage < 1
            if !firstPage {
                Button {
                    currentPage = 0
                    executeSearch()
                } label: {
                    Image(systemName: "chevron.backward.to.line")
                }
                Button {
                    currentPage -= 1
                    executeSearch()
                } label: {
                    Image(systemName: "chevron.backward")
                }
            }
            if pageCount != nil {
                let lastPage = currentPage >= pageCount! - 1
                if !lastPage {
                    Button {
                        currentPage += 1
                        executeSearch()
                    } label: {
                        Image(systemName: "chevron.forward")
                    }
                    Button {
                        currentPage = pageCount! - 1
                        executeSearch()
                    } label: {
                        Image(systemName: "chevron.forward.to.line")
                    }
                }
            } else {
                Button {
                    currentPage += 1
                    executeSearch()
                } label: {
                    Image(systemName: "chevron.forward")
                }
            }
        })
    }
    
    func executeSearch() {
        env.api.search(query: query, page: currentPage) { response, error in
            DispatchQueue.main.async {
                if let searchResult = response {
                    posts = searchResult.posts
                    fullCount = searchResult.full_count?.int64Value
                    pageCount = searchResult.pages?.int64Value
                }
                if let error = error {
                    self.isError = true
                    let kotlinError = (error as NSError).kotlinException
                    if kotlinError is Api.InvalidHttpResponseException {
                        let status = (kotlinError as! Api.InvalidHttpResponseException).status
                        if status == 400 {
                            errorCode = "Invalid Query"
                        } else {
                            Logger().error("Search Failed with status \(status): \(error.localizedDescription)")
                            errorCode = "Search Failed"
                        }
                    } else {
                        Logger().error("Search Failed with unkown error: \(error.localizedDescription)")
                        errorCode = "Search Failed"
                    }
                }
            }
        }
    }
}

struct GridItemView: View {
    
    var post: Api.PostQueryObject
    
    var width: CGFloat
    
    var body: some View {
        let thumbnailUrl = getThumbUrl(post: post)
        ZStack(alignment: .topTrailing) {
            if (thumbnailUrl != nil) {
                AsyncImage(url: thumbnailUrl) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    ProgressView()
                }
                .frame(width: width, height: width * 0.5625)
            } else {
                Image("logo512")
                    .resizable()
                    .scaledToFit()
                    .frame(width: width, height: width * 0.5625)
            }
        }
    }
    
    func getThumbUrl(post: Api.PostQueryObject) -> URL? {
        if post.thumbnail_url != nil {
            return URL(string: post.thumbnail_url!)
        } else if post.thumbnail_object_key != nil {
            return URL(string: Api.companion.BASE_URL + "get-object/" + post.thumbnail_object_key!)
        } else {
            return nil
        }
    }
    
}

extension Api.PostQueryObject: Identifiable {
}

struct SearchView<Content: View>: View {
    @Environment(\.isSearching) var isSearching
    @Environment(\.dismissSearch) var dismissSearch
    
    let content: (Bool) -> Content
    
    var body: some View {
        content(isSearching)
    }
    
    init(@ViewBuilder content: @escaping (Bool) -> Content) {
        self.content = content
    }
}
