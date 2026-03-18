//
//  ClientAIConversationRequest.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/22.
//

import Foundation
import TUICore
import CryptoKit

class ClientAIConversationRequest: AIConversationRequest {
    func sha256(msg: String) -> String {
        guard let data = msg.data(using: .utf8) else { return "" }
        let digest = SHA256.hash(data: data)
        return digest.compactMap{String(format: "%02x", $0)}.joined()
    }
    
    override func start(completion: @escaping (_ taskId: String, _ robotId: String?) -> Void) {
        Logger.info("startConversation userId:\(TUILogin.getUserID() ?? "")"
                    + " aiRobotId:\(config?.agentConfig?.aiRobotId ?? "")")
        
        guard let secretId = config?.secretId, !secretId.isEmpty else {
            completion("", nil)
            return
        }
        guard let secretKey = config?.secretKey, !secretKey.isEmpty else {
            completion("", nil)
            return
        }
        let token = ""
        
        let service = "trtc"
        let host = "trtc.tencentcloudapi.com"
        let endpoint = "https://\(host)"
        let action = "StartAIConversation"
        let version = "2019-07-22"
        let algorithm = "TC3-HMAC-SHA256"
        let timestamp = Int(Date().timeIntervalSince1970)
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        dateFormatter.timeZone = TimeZone(identifier: "UTC")
        let date = dateFormatter.string(from: Date(timeIntervalSince1970: TimeInterval(timestamp)))
        
        // ************* step1 *************
        let httpRequestMethod = "POST"
        let canonicalUri = "/"
        let canonicalQuerystring = ""
        let ct = "application/json; charset=utf-8"
        guard let payload = aiParamsConvert2Json(config: config) else {
            completion("", nil)
            return
        }
        let canonicalHeaders = "content-type:\(ct)\nhost:\(host)\n"
        let signedHeaders = "content-type;host"
        let hashedRequestPayload = sha256(msg: payload)
        let canonicalRequest = httpRequestMethod + "\n"
        + canonicalUri + "\n"
        + canonicalQuerystring + "\n"
        + canonicalHeaders + "\n"
        + signedHeaders + "\n"
        + hashedRequestPayload
        
        // ************* step2 *************
        let credentialScope = "\(date)/\(service)/tc3_request"
        let hashedCanonicalRequest = sha256(msg: canonicalRequest)
        let stringToSign = [algorithm, String(timestamp), credentialScope, hashedCanonicalRequest].joined(separator: "\n")
        
        // ************* step3 *************
        let keyData = Data("TC3\(secretKey)".utf8)
        let dateData = Data(date.utf8)
        var symmetricKey = SymmetricKey(data: keyData)
        let secretDate = HMAC<SHA256>.authenticationCode(for: dateData, using: symmetricKey)
        
        let serviceData = Data(service.utf8)
        symmetricKey = SymmetricKey(data: Data(secretDate))
        let secretService = HMAC<SHA256>.authenticationCode(for: serviceData, using: symmetricKey)
        
        let signingData = Data("tc3_request".utf8)
        symmetricKey = SymmetricKey(data: secretService)
        let secretSigning = HMAC<SHA256>.authenticationCode(for: signingData, using: symmetricKey)
        
        let stringToSignData = Data(stringToSign.utf8)
        symmetricKey = SymmetricKey(data: secretSigning)
        let signature = HMAC<SHA256>.authenticationCode(for: stringToSignData, using: symmetricKey).map{String(format: "%02hhx", $0)}.joined()
        
        // ************* step4 Authorization *************
        let authorization = "\(algorithm) Credential=\(secretId)/\(credentialScope), SignedHeaders=\(signedHeaders), Signature=\(signature)"
        
        // ************* step5 start *************
        guard let url = URL(string: endpoint) else {
            completion("", nil)
            return
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.httpBody = Data(payload.utf8)
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.setValue(authorization, forHTTPHeaderField: "Authorization")
        request.setValue(action, forHTTPHeaderField: "X-TC-Action")
        request.setValue(version, forHTTPHeaderField: "X-TC-Version")
        request.setValue(config?.region ?? "ap-beijing", forHTTPHeaderField: "X-TC-Region")
        request.setValue(String(timestamp), forHTTPHeaderField: "X-TC-Timestamp")
        request.setValue(token, forHTTPHeaderField: "X-TC-Token")
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            Logger.info("startConversation result data:\(String(describing: data)) response:\(String(describing: response)) error:\(String(describing: error))")
            if let error = error {
                Logger.error("startConversation network error:\(error.localizedDescription)")
                completion("", nil)
                return
            }
            if let data = data {
                do {
                    guard let jsonObject = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] else {
                        Logger.info("startConversation parse jsonObject failed")
                        completion("", nil)
                        return
                    }
                    if let response = jsonObject["Response"] as? [String: Any],
                       let taskId = response["TaskId"] as? String {
                        Logger.info("startConversation result taskId:\(taskId)")
                        completion(taskId, nil)
                    } else {
                        Logger.error("startConversation get taskId failed, response:\(String(describing: String(data: data, encoding: .utf8)))")
                        completion("", nil)
                    }
                } catch {
                    Logger.error("startConversation catch error:\(error.localizedDescription)")
                    completion("", nil)
                }
            } else {
                completion("", nil)
            }
        }
        
        task.resume()
    }
    
    override func stop(taskID: String) {
        Logger.info("stopConversation taskId:\(taskID)")
        guard let secretId = config?.secretId, !secretId.isEmpty else { return }
        guard let secretKey = config?.secretKey, !secretKey.isEmpty else { return }
        let token = ""
        
        let service = "trtc"
        let host = "trtc.tencentcloudapi.com"
        let endpoint = "https://\(host)"
        let action = "StopAIConversation"
        let version = "2019-07-22"
        let algorithm = "TC3-HMAC-SHA256"
        let timestamp = Int(Date().timeIntervalSince1970)
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        dateFormatter.timeZone = TimeZone(identifier: "UTC")
        let date = dateFormatter.string(from: Date(timeIntervalSince1970: TimeInterval(timestamp)))
        let httpRequestMethod = "POST"
        let canonicalUri = "/"
        let canonicalQuerystring = ""
        let ct = "application/json; charset=utf-8"
        let payload = "{\"TaskId\":\"\(taskID)\"}"
        let canonicalHeaders = "content-type:\(ct)\nhost:\(host)\n"
        let signedHeaders = "content-type;host"
        let hashedRequestPayload = sha256(msg: payload)
        let canonicalRequest = [httpRequestMethod, canonicalUri, canonicalQuerystring, canonicalHeaders, signedHeaders, hashedRequestPayload].joined(separator: "\n")
        let credentialScope = "\(date)/\(service)/tc3_request"
        let hashedCanonicalRequest = sha256(msg: canonicalRequest)
        let stringToSign = [algorithm, String(timestamp), credentialScope, hashedCanonicalRequest].joined(separator: "\n")
        
        // ************* step3 *************
        let keyData = Data("TC3\(secretKey)".utf8)
        let dateData = Data(date.utf8)
        var symmetricKey = SymmetricKey(data: keyData)
        let secretDate = HMAC<SHA256>.authenticationCode(for: dateData, using: symmetricKey)
        
        let serviceData = Data(service.utf8)
        symmetricKey = SymmetricKey(data: Data(secretDate))
        let secretService = HMAC<SHA256>.authenticationCode(for: serviceData, using: symmetricKey)
        
        let signingData = Data("tc3_request".utf8)
        symmetricKey = SymmetricKey(data: secretService)
        let secretSigning = HMAC<SHA256>.authenticationCode(for: signingData, using: symmetricKey)
        
        let stringToSignData = Data(stringToSign.utf8)
        symmetricKey = SymmetricKey(data: secretSigning)
        let signature = HMAC<SHA256>.authenticationCode(for: stringToSignData, using: symmetricKey).map{String(format: "%02hhx", $0)}.joined()
        let authorization = "\(algorithm) Credential=\(secretId)/\(credentialScope), SignedHeaders=\(signedHeaders), Signature=\(signature)"
        guard let url = URL(string: endpoint) else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.httpBody = Data(payload.utf8)
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.setValue(authorization, forHTTPHeaderField: "Authorization")
        request.setValue(action, forHTTPHeaderField: "X-TC-Action")
        request.setValue(version, forHTTPHeaderField: "X-TC-Version")
        request.setValue(config?.region ?? "ap-beijing", forHTTPHeaderField: "X-TC-Region")
        request.setValue(String(timestamp), forHTTPHeaderField: "X-TC-Timestamp")
        request.setValue(token, forHTTPHeaderField: "X-TC-Token")
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            Logger.info("stopConversation result data:\(String(describing: data)) response:\(String(describing: response)) error:\(String(describing: error))")
        }
        
        task.resume()
    }
    
    func aiParamsConvert2Json(config: AIConversationConfig?) -> String? {
        guard let config else { return nil }
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        do {
            let jsonData = try encoder.encode(config)
            guard var jsonDict = try JSONSerialization.jsonObject(with: jsonData) as? [String: Any] else { return nil }
            jsonDict["RoomId"] = roomId
            jsonDict["RoomIdType"] = 1
            jsonDict["SdkAppId"] = Int(TUILogin.getSdkAppID())
            if var agentDict = jsonDict["AgentConfig"] as? [String: Any] {
                agentDict["TargetUserId"] = TUILogin.getUserID() ?? ""
                jsonDict["AgentConfig"] = agentDict
            }
            let resultData = try JSONSerialization.data(withJSONObject: jsonDict, options: [.sortedKeys])
            return String(data: resultData, encoding: .utf8)
        } catch {
            Logger.error("aiParamsConvert2Json failed: \(error)")
            return nil
        }
    }
    
}
