//
//  MainLocalized.swift
//  TRTCScene
//
//  Created by adams on 2021/5/10.
//  Copyright © 2022 Tencent. All rights reserved.

import Foundation
import TUICore

let kitName = "AIConversationKit"
let AIConversationTable = "AIConversationLocalized"

func AIConversationLocalize(_ key: String) -> String {
    if let bundlePath = bundle().path(forResource: TUIGlobalization.getPreferredLanguage() ?? "", ofType: "lproj"),
       let bundle = Bundle(path: bundlePath) {
        return bundle.localizedString(forKey: key, value: "", table: AIConversationTable)
    }
    return AILocalized.sharedBundle.localizedString(forKey: key, value: "", table: AIConversationTable)
}

func bundle() -> Bundle {
    return AILocalized.sharedBundle
}

class AILocalized {
    class var sharedBundle: Bundle {
        struct Static {
            static let bundle: Bundle? = aiBundle()
        }
        guard let bundle = Static.bundle else {
            return Bundle()
        }
        return bundle
    }
    
    static func isChineseLocale() -> Bool {
        return TUIGlobalization.getPreferredLanguage().hasPrefix("zh")
    }
}


func aiBundle() -> Bundle? {
    var url: NSURL? = Bundle.main.url(forResource: "\(kitName)Bundle", withExtension: "bundle") as NSURL?
    if let associateBundleURL = url {
        return Bundle(url: associateBundleURL as URL)
    }
    url = Bundle.main.url(forResource: "Frameworks", withExtension: nil) as NSURL?
    url = url?.appendingPathComponent(kitName) as NSURL?
    url = url?.appendingPathComponent("framework") as NSURL?
    if let associateBundleURL = url {
        let bundle = Bundle(url: associateBundleURL as URL)
        url = bundle?.url(forResource: "\(kitName)Bundle", withExtension: "bundle") as NSURL?
        if let associateBundleURL = url {
            return Bundle(url: associateBundleURL as URL)
        }
    }
    return nil
}

extension UIImage {
    convenience init?(inAIBundleNamed imageName: String) {
        self.init(named: imageName,
                  in: AILocalized.sharedBundle,
                  compatibleWith: nil)
    }
}
