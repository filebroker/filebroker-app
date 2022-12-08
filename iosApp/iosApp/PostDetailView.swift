//
//  PostDetailView.swift
//  iosApp
//
//  Created by Robin Friedli on 08.12.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import AVKit
import os
import shared
import SwiftUI

struct PostDetailView: View {
    
    @ObservedObject var env: Env
    
    @State var postKey: Int32
    var query: String
    var currentPage: Int64
    
    @State var postDetailed: Api.PostDetailed? = nil
    
    @State var isError = false
    @State var errorCode: String? = nil
    
    var body: some View {
        VStack {
            if postDetailed != nil {
                HStack {
                    if postDetailed!.s3_object != nil && postDetailed!.s3_object!.mime_type.starts(with: "image") {
                        AsyncImage(url: URL(string: Api.companion.BASE_URL + "get-object/" + postDetailed!.s3_object!.object_key)) { image in
                            image
                                .resizable()
                                .scaledToFit()
                        } placeholder: {
                            ProgressView()
                        }
                        .frame(alignment: .center)
                    } else if postDetailed!.s3_object != nil && postDetailed!.s3_object!.mime_type.starts(with: "video") {
                        VideoPlayer(player: AVPlayer(url: URL(string: Api.companion.BASE_URL + "get-object/" + postDetailed!.s3_object!.object_key)!))
                            .frame(alignment: .center)
                    } else {
                        Text("Cannot display post data")
                    }
                }
                
                HStack {
                    Text("Tags")
                        .frame(alignment: .leading)
                    Spacer()
                    Text(postDetailed!.tags.map { tag in tag.tag_name }.joined(separator: ", "))
                        .frame(alignment: .trailing)
                }
                
                if postDetailed!.title != nil && !postDetailed!.title!.isEmpty {
                    Text(postDetailed!.title!)
                        .font(.title)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                if postDetailed!.description_ != nil && !postDetailed!.description_!.isEmpty {
                    Text(postDetailed!.description_!)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Spacer()
            } else {
                ProgressView()
            }
        }
        .onAppear {
            loadPost()
        }
        .alert("Error", isPresented: $isError, actions: {}, message: {Text(self.errorCode ?? "")})
        .navigationBarTitle("Post", displayMode: .inline)
        .toolbar {
            ToolbarItemGroup(placement: .bottomBar) {
                Spacer()
                if postDetailed != nil && postDetailed!.prev_post_pk != nil {
                    Button {
                        postKey = postDetailed!.prev_post_pk!.int32Value
                        postDetailed = nil
                        loadPost()
                    } label: {
                        Image(systemName: "chevron.backward")
                    }
                }
                if postDetailed != nil && postDetailed!.next_post_pk != nil {
                    Button {
                        postKey = postDetailed!.next_post_pk!.int32Value
                        postDetailed = nil
                        loadPost()
                    } label: {
                        Image(systemName: "chevron.forward")
                    }
                }
            }
        }
    }
    
    func loadPost() {
        env.api.getPost(key: postKey, query: query, page: currentPage) { postDetailed, error in
            DispatchQueue.main.async {
                if let postDetailed = postDetailed {
                    self.postDetailed = postDetailed
                }
                if let error = error {
                    self.isError = true
                    let kotlinError = (error as NSError).kotlinException
                    if kotlinError is Api.InvalidHttpResponseException {
                        let status = (kotlinError as! Api.InvalidHttpResponseException).status
                        if status == 400 {
                            errorCode = "Invalid Query"
                        } else {
                            Logger().error("Request failed with status \(status): \(error.localizedDescription)")
                            errorCode = "Request failed"
                        }
                    } else {
                        Logger().error("Request failed with unkown error: \(error.localizedDescription)")
                        errorCode = "Request failed"
                    }
                }
            }
        }
    }
}
