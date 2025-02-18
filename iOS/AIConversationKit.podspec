Pod::Spec.new do |spec|
    spec.name = 'AIConversationKit'
    spec.version = '1.0.0'
    spec.license = { :type => 'MIT' }
    spec.homepage = 'https://cloud.tencent.com/product/trtc'
    spec.authors = 'tencent video cloud'
    spec.summary = 'AI交谈组件'
    spec.ios.deployment_target = '9.0'
    spec.dependency 'SDWebImage'
    spec.dependency 'Masonry'
    spec.static_framework = true
    spec.dependency 'TUICore'
    spec.default_subspec = 'Professional'
    spec.dependency 'RTCCommon'
    spec.dependency 'Kingfisher'
    spec.source  = { :path => './' }
    spec.subspec "Professional" do |s|
        s.dependency 'TXLiteAVSDK_Professional'
        s.source_files = 'AIConversationKit/**/*.{h,m,swift}'
        s.resource_bundles = {
           'AIConversationKitBundle' => ['Resource/**/*','AIConversationKit/AILocalized/**/*']
        }
        s.pod_target_xcconfig = {'HEADER_SEARCH_PATHS' =>['${PODS_ROOT}/TXLiteAVSDK_Professional/TXLiteAVSDK_Professional/TXLiteAVSDK_Professional.xcframework/ios-arm64_armv7/TXLiteAVSDK_Professional.framework/Headers/']}
    end
    
    spec.subspec "TRTC" do |s|
        s.dependency 'TXLiteAVSDK_TRTC'
        s.source_files = 'AIConversationKit/**/*.{h,m,swift}'
        s.resource_bundles = {
           'AIConversationKitBundle' => ['Resource/**/*','AIConversationKit/AILocalized/**/*']
        }
        s.pod_target_xcconfig = {'HEADER_SEARCH_PATHS' =>['${PODS_ROOT}/TXLiteAVSDK_Player/TXLiteAVSDK_TRTC/TXLiteAVSDK_Player.xcframework/ios-arm64_armv7/TXLiteAVSDK_TRTC.framework/Headers/']}
    end
end
