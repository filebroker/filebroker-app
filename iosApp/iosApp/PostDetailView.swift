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
import VLCUI

struct PostDetailView: View {
    
    @ObservedObject var env: Env
    
    @State var postKey: Int32
    var query: String
    @Binding var currentPage: Int64
    
    @State var postDetailed: Api.PostDetailed? = nil
    
    @State var showSettingsPopOver = false
    @State var isError = false
    @State var errorCode: String? = nil
    
    @State var videoPlayer: AVPlayer? = nil
    @State var vlcPlayerConfiguration: VLCVideoPlayer.Configuration? = nil
    
    @State var isVideo = false
    @State var hlsAvailable = false
    @State var hlsEnabled = false
    @State var nativePlayerEnabled = false
    
    var body: some View {
        VStack {
            if postDetailed != nil {
                HStack {
                    if postDetailed!.s3_object != nil && postDetailed!.s3_object!.mime_type.starts(with: "image") {
                        let objectUrl = Api.companion.BASE_URL + "get-object/" + postDetailed!.s3_object!.object_key
                        AsyncImage(url: URL(string: objectUrl)) { image in
                            ShareLink(item: image, preview: SharePreview("Photo", image: image)) {
                                image
                                    .resizable()
                                    .scaledToFit()
                            }
                        } placeholder: {
                            ProgressView()
                        }
                        .frame(alignment: .center)
                    } else if videoPlayer != nil || vlcPlayerConfiguration != nil {
                        if videoPlayer != nil {
                            NativeVideoPlayer(player: videoPlayer!)
                        } else if vlcPlayerConfiguration != nil {
                            VlcVideoPlayer(vlcPlayerConfiguration: vlcPlayerConfiguration!)
                        }
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
            // may be called after coming out of fullscreen
            if postDetailed == nil {
                loadPost()
            }
        }
        .alert("Error", isPresented: $isError, actions: {}, message: {Text(self.errorCode ?? "")})
        .navigationBarTitle("Post", displayMode: .inline)
        .toolbar {
            if isVideo {
                ToolbarItemGroup(placement: .navigation) {
                    Button {
                        showSettingsPopOver.toggle()
                    } label: {
                        Image(systemName: "ellipsis")
                    }
                    .sheet(isPresented: $showSettingsPopOver) {
                        HStack{
                            Button {
                                showSettingsPopOver = false
                            } label: {
                                Image(systemName: "xmark.circle")
                                    .foregroundColor(.gray)
                                    .padding(10)
                            }
                            Spacer()
                        }
                        List {
                            if hlsAvailable {
                                Toggle("HLS", isOn: $hlsEnabled)
                                    .onChange(of: hlsEnabled) { value in
                                        nativePlayerEnabled = value
                                        if postDetailed != nil {
                                            setVideoPlayer(postDetailed: postDetailed!)
                                        }
                                    }
                            }
                            Toggle("Use Native Player", isOn: $nativePlayerEnabled)
                                .onChange(of: nativePlayerEnabled) { value in
                                    if postDetailed != nil {
                                        setVideoPlayer(postDetailed: postDetailed!)
                                    }
                                }
                        }
                        .listStyle(.insetGrouped)
                    }
                }
            }
            ToolbarItemGroup(placement: .bottomBar) {
                Spacer()
                if postDetailed != nil && postDetailed!.prev_post != nil {
                    Button {
                        postKey = postDetailed!.prev_post!.pk
                        currentPage = postDetailed!.prev_post!.page
                        postDetailed = nil
                        videoPlayer?.pause()
                        loadPost()
                    } label: {
                        Image(systemName: "chevron.backward")
                    }
                }
                if postDetailed != nil && postDetailed!.next_post != nil {
                    Button {
                        postKey = postDetailed!.next_post!.pk
                        currentPage = postDetailed!.next_post!.page
                        postDetailed = nil
                        videoPlayer?.pause()
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
                    self.isVideo = postDetailed.s3_object != nil && postDetailed.s3_object!.mime_type.starts(with: "video")
                    self.hlsAvailable = postDetailed.s3_object != nil && postDetailed.s3_object!.hls_master_playlist != nil
                    self.hlsEnabled = self.hlsAvailable
                    self.nativePlayerEnabled = self.hlsAvailable
                    setVideoPlayer(postDetailed: postDetailed)
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
    
    func setVideoPlayer(postDetailed: Api.PostDetailed) {
        let url = self.hlsAvailable && self.hlsEnabled
        ? URL(string: Api.companion.BASE_URL + "get-object/" + postDetailed.s3_object!.hls_master_playlist!)!
        : URL(string: Api.companion.BASE_URL + "get-object/" + postDetailed.s3_object!.object_key)!
        
        if self.isVideo && self.nativePlayerEnabled {
            self.videoPlayer = AVPlayer(url: url)
            self.vlcPlayerConfiguration = nil
        } else if self.isVideo {
            self.videoPlayer = nil
            self.vlcPlayerConfiguration = getVlcConfiguration(url: url)
        } else {
            self.videoPlayer = nil
            self.vlcPlayerConfiguration = nil
        }
    }
    
    func getVlcConfiguration(url: URL) -> VLCVideoPlayer.Configuration {
        let vlcConfiguration = VLCVideoPlayer.Configuration(url: url)
        vlcConfiguration.autoPlay = true
        return vlcConfiguration
    }
}
