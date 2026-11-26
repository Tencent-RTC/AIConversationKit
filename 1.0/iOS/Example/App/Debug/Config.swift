//
//  DemoTokenManager.swift
//  AIConversationApp
//
//  Created by einhorn on 2025/2/8.
//

import UIKit
import AIConversationKit
import TUICore

let SECRET_ID = ""
let SECRET_KEY = ""

let tokenData: [String: Any]? = {
    guard let url = Bundle.main.url(forResource: "Config", withExtension: "json"),
          let data = try? Data(contentsOf: url),
          let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
        print("Config.json not found or parse failed")
        return nil
    }
    return json
}()

func getAIConversationConfig() -> AIConversationConfig? {
    guard let tokenDict = tokenData,
          let chatConfig = tokenDict["chatConfig"] as? [String: Any],
          let userInfo = tokenDict["userInfo"] as? [String: Any],
          let sdkAppId = chatConfig["SdkAppId"] as? Int,
          let userId = userInfo["userId"] as? String,
          let userSig = userInfo["userSig"] as? String else { return nil }
    
    TUILogin.login(Int32(sdkAppId), userID: userId, userSig: userSig) {} fail: { code, err in
        debugPrint("login failed: \(code) errMsg:\(err ?? "")")
    }
    
    let keys = ["AgentConfig", "STTConfig", "LLMConfig", "TTSConfig"]
    let filteredConfig = chatConfig.filter { keys.contains($0.key) }
    guard let jsonData = try? JSONSerialization.data(withJSONObject: filteredConfig),
          var config = try? JSONDecoder().decode(AIConversationConfig.self, from: jsonData) else {
        print("parse AIConversationConfig failed")
        return nil
    }
    config.secretId = SECRET_ID
    config.secretKey = SECRET_KEY
    return config
}
