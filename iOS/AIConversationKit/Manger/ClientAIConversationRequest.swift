//
//  ClientAIConversationService.swift
//  AFNetworking
//
//  Created by einhorn on 2024/10/22.
//

import UIKit
import TUICore
import CryptoKit



class ClientAIConversationRequest: AIConversationRequest {
    func sha256(msg: String) -> String {
        guard let data = msg.data(using: .utf8) else { return "" }
        let digest = SHA256.hash(data: data)
        return digest.compactMap{String(format: "%02x", $0)}.joined()
    }
    
    override func start(completion: @escaping (String) -> Void) {
      
        guard let secretId =  startAiConversationParams?.secretId else { return }
        guard let secretKey =  startAiConversationParams?.secretKey else { return }
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
        guard let payload  = aiParamsConvert2Json(aiParams: startAiConversationParams) else { return }
        let canonicalHeaders = "content-type:\(ct)\nhost:\(host)\nx-tc-action:\(action.lowercased())\n"
        let signedHeaders = "content-type;host;x-tc-action"
        let hashedRequestPayload = sha256(msg: payload)
        let canonicalRequest = """
        \(httpRequestMethod)
        \(canonicalUri)
        \(canonicalQuerystring)
        \(canonicalHeaders)
        \(signedHeaders)
        \(hashedRequestPayload)
        """
        print(canonicalRequest)

        // ************* step2 *************
        let credentialScope = "\(date)/\(service)/tc3_request"
        let hashedCanonicalRequest = sha256(msg: canonicalRequest)
        let stringToSign = """
        \(algorithm)
        \(timestamp)
        \(credentialScope)
        \(hashedCanonicalRequest)
        """


//        // ************* step3 *************
        let keyData = Data("TC3\(secretKey)".utf8)
        let dateData = Data(date.utf8)
        var symmetricKey = SymmetricKey(data: keyData)
        let secretDate = HMAC<SHA256>.authenticationCode(for: dateData, using: symmetricKey)
        let secretDateString = Data(secretDate).map{String(format: "%02hhx", $0)}.joined()
        print("\(secretDateString)")

        let serviceData = Data(service.utf8)
        symmetricKey = SymmetricKey(data: Data(secretDate))
        let secretService = HMAC<SHA256>.authenticationCode(for: serviceData, using: symmetricKey)
        let secretServiceString = Data(secretService).map{String(format: "%02hhx", $0)}.joined()
        print("\(secretServiceString)")

        let signingData = Data("tc3_request".utf8)
        symmetricKey = SymmetricKey(data: secretService)
        let secretSigning = HMAC<SHA256>.authenticationCode(for: signingData, using: symmetricKey)
        let secretSigningString = Data(secretSigning).map{String(format: "%02hhx", $0)}.joined()
        print("\(secretSigningString)")

        let stringToSignData = Data(stringToSign.utf8)
        symmetricKey = SymmetricKey(data: secretSigning)
        let signature = HMAC<SHA256>.authenticationCode(for: stringToSignData, using: symmetricKey).map{String(format: "%02hhx", $0)}.joined()
        print(signature)

        // ************* step4 Authorization *************
        let authorization = """
        \(algorithm) Credential=\(secretId)/\(credentialScope), SignedHeaders=\(signedHeaders), Signature=\(signature)
        """
        print(authorization)

        // ************* step5 start *************
        guard let url = URL(string: endpoint) else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.httpBody = Data(payload.utf8)
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.setValue(authorization, forHTTPHeaderField: "Authorization")
        request.setValue(action, forHTTPHeaderField: "X-TC-Action")
        request.setValue(version, forHTTPHeaderField: "X-TC-Version")
        request.setValue(startAiConversationParams?.region, forHTTPHeaderField: "X-TC-Region")
        request.setValue(String(timestamp), forHTTPHeaderField: "X-TC-Timestamp")
        request.setValue(token, forHTTPHeaderField: "X-TC-Token")
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            if let error = error {
                print("Error: \(error)")
                return
            }
            if let data = data {
                do {
                    if let jsonObject = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
                       let response = jsonObject["Response"] as? [String: Any],
                       let taskId = response["TaskId"] as? String {
                        print("TaskId: \(taskId)")
                        completion(taskId)
                    } else {
                        print("Error: Unable to find TaskId")
                    }
                } catch {
                    print("Error: \(error.localizedDescription)")
                }
            }
        }

        task.resume()
       
    }
    
    override func stop(taskID: String) {
        guard let secretId =  startAiConversationParams?.secretId else { return }
        guard let secretKey =  startAiConversationParams?.secretKey else { return }
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
        let canonicalHeaders = "content-type:\(ct)\nhost:\(host)\nx-tc-action:\(action.lowercased())\n"
        let signedHeaders = "content-type;host;x-tc-action"
        let hashedRequestPayload = sha256(msg: payload)
        let canonicalRequest = """
        \(httpRequestMethod)
        \(canonicalUri)
        \(canonicalQuerystring)
        \(canonicalHeaders)
        \(signedHeaders)
        \(hashedRequestPayload)
        """
        print(canonicalRequest)
        let credentialScope = "\(date)/\(service)/tc3_request"
        let hashedCanonicalRequest = sha256(msg: canonicalRequest)
        let stringToSign = """
        \(algorithm)
        \(timestamp)
        \(credentialScope)
        \(hashedCanonicalRequest)
        """
        print(stringToSign)

        // ************* step3 *************
        let keyData = Data("TC3\(secretKey)".utf8)
        let dateData = Data(date.utf8)
        var symmetricKey = SymmetricKey(data: keyData)
        let secretDate = HMAC<SHA256>.authenticationCode(for: dateData, using: symmetricKey)
        let secretDateString = Data(secretDate).map{String(format: "%02hhx", $0)}.joined()
        print("\(secretDateString)")

        let serviceData = Data(service.utf8)
        symmetricKey = SymmetricKey(data: Data(secretDate))
        let secretService = HMAC<SHA256>.authenticationCode(for: serviceData, using: symmetricKey)
        let secretServiceString = Data(secretService).map{String(format: "%02hhx", $0)}.joined()
        print("\(secretServiceString)")

        let signingData = Data("tc3_request".utf8)
        symmetricKey = SymmetricKey(data: secretService)
        let secretSigning = HMAC<SHA256>.authenticationCode(for: signingData, using: symmetricKey)
        let secretSigningString = Data(secretSigning).map{String(format: "%02hhx", $0)}.joined()
        print("\(secretSigningString)")

        let stringToSignData = Data(stringToSign.utf8)
        symmetricKey = SymmetricKey(data: secretSigning)
        let signature = HMAC<SHA256>.authenticationCode(for: stringToSignData, using: symmetricKey).map{String(format: "%02hhx", $0)}.joined()
        print(signature)
        let authorization = """
        \(algorithm) Credential=\(secretId)/\(credentialScope), SignedHeaders=\(signedHeaders), Signature=\(signature)
        """
        print(authorization)
        guard let url = URL(string: endpoint) else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.httpBody = Data(payload.utf8)
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.setValue(authorization, forHTTPHeaderField: "Authorization")
        request.setValue(action, forHTTPHeaderField: "X-TC-Action")
        request.setValue(version, forHTTPHeaderField: "X-TC-Version")
        request.setValue(startAiConversationParams?.region, forHTTPHeaderField: "X-TC-Region")
        request.setValue(String(timestamp), forHTTPHeaderField: "X-TC-Timestamp")
        request.setValue(token, forHTTPHeaderField: "X-TC-Token")
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            if let error = error {
                print("Error: \(error)")
                return
            }
            if let data = data {
                if let responseString = String(data: data, encoding: .utf8) {
                    print("Response: \(responseString)")
                }
            }
        }

        task.resume()
    }
    
    func aiParamsConvert2Json(aiParams: StartAIConversationParams?) -> String? {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .prettyPrinted
        if let jsonData = try? encoder.encode(aiParams),
           let jsonString = String(data: jsonData, encoding: .utf8) {
            return jsonString
        }
        return ""
    }

}

