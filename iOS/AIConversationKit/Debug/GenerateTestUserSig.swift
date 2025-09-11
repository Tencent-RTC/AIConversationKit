/*
 * Module: GenerateTestUserSig
 *
 * Function: used to generate UserSig for testing. UserSig is a security protection signature designed by Tencent Cloud for its cloud services.
 * The calculation method is to encrypt SDKAppID, UserID and EXPIRETIME, and the encryption algorithm is HMAC-SHA256.
 *
 * Attention: Please do not publish the following code into your online official version of the App for the following reasons:
 *
 * Although the code in this file can correctly calculate UserSig,
 * it is only suitable for quickly adjusting the basic functions of the SDK and is not suitable for online products.
 * This is because the SDKSECRETKEY in the client code is easily decompiled and reverse-engineered, especially the web-side code,
 * which is almost zero difficulty in cracking.
 * Once your key is leaked, an attacker can calculate the correct UserSig to steal your Tencent Cloud traffic.
 *
 * The correct approach is to put the UserSig calculation code and encryption key on your business server,
 * and then have the App obtain the real-time calculated UserSig from your server on demand.
 * Since it is more expensive to crack a server than a client app, a server-computed approach better protects your encryption keys.
 *
 * Reference: https://cloud.tencent.com/document/product/269/32688#Server
 */

import Foundation
import CommonCrypto
import zlib


 
let EXPIRETIME: Int = 604800

public class GenerateTestUserSig {
    
    class func hmac(_ plainText: String, secretKey: String) -> String? {
        let cData = plainText.cString(using: String.Encoding.utf8)

        let cKeyLen = secretKey.lengthOfBytes(using: .utf8)
        let cDataLen = plainText.lengthOfBytes(using: .utf8)

        var cHMAC = [CUnsignedChar].init(repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        let pointer = cHMAC.withUnsafeMutableBufferPointer { (unsafeBufferPointer) in
            return unsafeBufferPointer
        }
        guard let cKey = secretKey.cString(using: String.Encoding.utf8) else { return "" }
        CCHmac(CCHmacAlgorithm(kCCHmacAlgSHA256), cKey, cKeyLen, cData, cDataLen, pointer.baseAddress)
        guard let baseAddress = pointer.baseAddress else { return "" }
        let data = Data.init(bytes: baseAddress, count: cHMAC.count)
        return data.base64EncodedString(options: [])
    }

    class func base64URL(data: Data) -> String {
        let result = data.base64EncodedString(options: Data.Base64EncodingOptions.init(rawValue: 0))
        var final = ""
        result.forEach { (char) in
            switch char {
            case "+":
                final += "*"
            case "/":
                final += "-"
            case "=":
                final += "_"
            default:
                final += "\(char)"
            }
        }
        return final
    }
    
    public class func genTestUserSig(sdkAppId: Int, userId: String, secrectkey: String) -> String {
        let current = CFAbsoluteTimeGetCurrent() + kCFAbsoluteTimeIntervalSince1970
        let TLSTime: CLong = CLong(floor(current))
        var obj: [String: Any] = [
            "TLS.ver": "2.0",
            "TLS.identifier": userId,
            "TLS.sdkappid":sdkAppId,
            "TLS.expire": EXPIRETIME,
            "TLS.time": TLSTime
        ]
        let keyOrder = [
            "TLS.identifier",
            "TLS.sdkappid",
            "TLS.time",
            "TLS.expire"
        ]
        var stringToSign = ""
        keyOrder.forEach { (key) in
            if let value = obj[key] {
                stringToSign += "\(key):\(value)\n"
            }
        }
        print("string to sign: \(stringToSign)")
        if let sig = hmac(stringToSign, secretKey: secrectkey) {
            obj["TLS.sig"] = sig
            print("sig: \(String(describing: sig))")
        }
        guard let jsonData = try? JSONSerialization.data(withJSONObject: obj, options: .sortedKeys) else { return "" }
        
        let bytes = jsonData.withUnsafeBytes { (result) -> UnsafePointer<Bytef> in
            if let baseAddress = result.bindMemory(to: Bytef.self).baseAddress {
                return baseAddress
            }
            return UnsafePointer<Bytef>.init("")
        }
        let srcLen: uLongf = uLongf(jsonData.count)
        let upperBound: uLong = compressBound(srcLen)
        let capacity: Int = Int(upperBound)
        let dest: UnsafeMutablePointer<Bytef> = UnsafeMutablePointer<Bytef>.allocate(capacity: capacity)
        var destLen = upperBound
        let ret = compress2(dest, &destLen, bytes, srcLen, Z_BEST_SPEED)
        if ret != Z_OK {
            print("[Error] Compress Error \(ret), upper bound: \(upperBound)")
            dest.deallocate()
            return ""
        }
        let count = Int(destLen)
        let result = self.base64URL(data: Data.init(bytesNoCopy: dest, count: count, deallocator: .free))
        return result
    }
}
