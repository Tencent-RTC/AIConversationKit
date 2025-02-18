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
var tokenData: [String: Any]? = {
    if let url = Bundle.main.url(forResource: "Config", withExtension: "json") {
        do {
    
            let data = try Data(contentsOf: url)
            do {

                let jsonObject = try JSONSerialization.jsonObject(with: data)
                return jsonObject as? [String: Any]
            } catch {
                print("JSON parse err: \(error)")
                return nil
            }
        } catch {
            print("can't read Config.json: \(error.localizedDescription)")
        }
    } else {
        print("Config.json not found")
    }
    return nil
}()




func getStartAIConversationParams() -> StartAIConversationParams? {
    guard let tokenDict = tokenData else { return nil }
    let chatConfig = tokenDict ["chatConfig"] as! [String : Any]
    let chatConfigJson = dictToJsonString(chatConfig)
    let userinfo = tokenDict ["userInfo"] as! [String : Any]
    
    guard let sdkappid = chatConfig ["SdkAppId"] as? Int32 else { return nil }
    guard let userId = userinfo ["userId"] as? String else { return nil }
    guard let userSig = userinfo ["userSig"] as? String else { return nil }
    TUILogin.login(sdkappid, userID: userId, userSig: userSig) {
        
    } fail: { code, err in
        debugPrint("login failed: \(code) errMsg:\(err ?? "")")
    }

    guard let jsonData = chatConfigJson.data(using: .utf8) else { return nil }
    
    do {
        let decoder = JSONDecoder()
        let config = try decoder.decode(StartAIConversationParams.self, from: jsonData)
        config.secretId = SECRET_ID
        config.secretKey = SECRET_KEY
        return config
    } catch {
        print("parse error: \(error)")
        return nil
    }
}



func convertJSONSDataToDictionary(_ jsonString: String) -> [String: Any]? {

    do {
        if let data = jsonString.data(using: .utf8) {
            let jsonObject = try JSONSerialization.jsonObject(with: data)
            return jsonObject as? [String: Any]
        }
    } catch {
        print("JSON parse error: \(error)")
        return nil
    }
    return nil
}


func dictToJsonString(_ dict: [String: Any]) -> String {
    do {
        let jsonData = try JSONSerialization.data(withJSONObject: dict)
        if let jsonString = String(data: jsonData, encoding: .utf8) {
            return jsonString
        }
    } catch {
        print("Error converting dictionary to JSON: \(error)")
    }
    return "{}" 
}
