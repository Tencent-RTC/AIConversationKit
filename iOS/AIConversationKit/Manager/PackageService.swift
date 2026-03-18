//
//  PackageService.swift
//  AIConversationKit
//
//  Created on 2026/2/26.
//

import Foundation

enum PackageService {
    private static let rtCubeBundleId = "com.tencent.mrtc"
    private static let tencentRTCBundleId = "com.tencent.rtc.app"

    static var isInternalDemo: Bool {
        isRTCube || isTencentRTC
    }

    static var isRTCube: Bool {
        Bundle.main.bundleIdentifier == rtCubeBundleId
    }

    static var isTencentRTC: Bool {
        Bundle.main.bundleIdentifier == tencentRTCBundleId
    }
}
