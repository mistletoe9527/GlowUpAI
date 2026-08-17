import AuthenticationServices
import CryptoKit
import Foundation
import Security
import UIKit

/// Google OAuth 登录后的用户资料。
struct GoogleSignInProfile {
    /// GlowUp 本地用户 ID。
    let userId: String
    /// Google 账号邮箱。
    let email: String
    /// Google 账号展示名。
    let name: String
    /// Google OpenID Connect ID token。
    let idToken: String
}

/// Google OAuth 登录服务。
@MainActor
final class GoogleOAuthService: NSObject, ASWebAuthenticationPresentationContextProviding {
    /// 当前浏览器登录会话。
    private var currentSession: ASWebAuthenticationSession?

    /// URLSession 实例。
    private let urlSession: URLSession

    /// App 配置来源。
    private let bundle: Bundle

    /// 创建 Google OAuth 登录服务。
    ///
    /// - Parameters:
    ///   - urlSession: URLSession 实例
    ///   - bundle: App 配置来源
    init(urlSession: URLSession = .shared, bundle: Bundle = .main) {
        self.urlSession = urlSession
        self.bundle = bundle
    }

    /// 发起 Google OAuth 登录。
    ///
    /// - Returns: Google 登录用户资料
    func signIn() async throws -> GoogleSignInProfile {
        let configuration = try GoogleOAuthConfiguration.load(from: bundle)
        let state = PKCE.randomString(length: 32)
        let codeVerifier = PKCE.randomString(length: 64)
        let callbackURL = try await authenticate(configuration: configuration, state: state, codeVerifier: codeVerifier)
        let code = try authorizationCode(from: callbackURL, expectedState: state)
        let token = try await exchangeCode(code, codeVerifier: codeVerifier, configuration: configuration)
        let payload = try GoogleIDTokenPayload.decode(from: token.idToken)
        return GoogleSignInProfile(
            userId: "google-\(payload.sub)",
            email: payload.email ?? "",
            name: payload.displayName,
            idToken: token.idToken
        )
    }

    /// 提供 ASWebAuthenticationSession 展示窗口。
    ///
    /// - Parameter session: 当前认证会话
    /// - Returns: 展示窗口
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }

    /// 通过系统浏览器完成 Google 授权。
    ///
    /// - Parameters:
    ///   - configuration: Google OAuth 配置
    ///   - state: 防跨站请求随机值
    ///   - codeVerifier: PKCE 校验码
    /// - Returns: Google 回调 URL
    private func authenticate(
        configuration: GoogleOAuthConfiguration,
        state: String,
        codeVerifier: String
    ) async throws -> URL {
        let authURL = try authorizationURL(configuration: configuration, state: state, codeVerifier: codeVerifier)
        return try await withCheckedThrowingContinuation { continuation in
            let session = ASWebAuthenticationSession(url: authURL, callbackURLScheme: configuration.redirectScheme) { [weak self] callbackURL, error in
                Task { @MainActor in
                    self?.currentSession = nil
                }
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                guard let callbackURL else {
                    continuation.resume(throwing: GoogleOAuthError.missingCallbackURL)
                    return
                }
                continuation.resume(returning: callbackURL)
            }
            session.presentationContextProvider = self
            session.prefersEphemeralWebBrowserSession = true
            currentSession = session
            guard session.start() else {
                currentSession = nil
                continuation.resume(throwing: GoogleOAuthError.sessionStartFailed)
                return
            }
        }
    }

    /// 构造 Google 授权 URL。
    ///
    /// - Parameters:
    ///   - configuration: Google OAuth 配置
    ///   - state: 防跨站请求随机值
    ///   - codeVerifier: PKCE 校验码
    /// - Returns: 授权 URL
    private func authorizationURL(
        configuration: GoogleOAuthConfiguration,
        state: String,
        codeVerifier: String
    ) throws -> URL {
        var components = URLComponents(string: "https://accounts.google.com/o/oauth2/v2/auth")
        components?.queryItems = [
            URLQueryItem(name: "client_id", value: configuration.clientID),
            URLQueryItem(name: "redirect_uri", value: configuration.redirectURI),
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "scope", value: "openid email profile"),
            URLQueryItem(name: "state", value: state),
            URLQueryItem(name: "code_challenge", value: PKCE.codeChallenge(from: codeVerifier)),
            URLQueryItem(name: "code_challenge_method", value: "S256"),
            URLQueryItem(name: "prompt", value: "select_account")
        ]
        guard let url = components?.url else {
            throw GoogleOAuthError.invalidAuthorizationURL
        }
        return url
    }

    /// 从回调 URL 读取授权码。
    ///
    /// - Parameters:
    ///   - callbackURL: Google 回调 URL
    ///   - expectedState: 发起登录时的 state
    /// - Returns: 授权码
    private func authorizationCode(from callbackURL: URL, expectedState: String) throws -> String {
        let components = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false)
        let queryItems = components?.queryItems ?? []
        if let error = queryItems.first(where: { $0.name == "error" })?.value {
            throw GoogleOAuthError.providerError(error)
        }
        let state = queryItems.first(where: { $0.name == "state" })?.value
        guard state == expectedState else {
            throw GoogleOAuthError.stateMismatch
        }
        guard let code = queryItems.first(where: { $0.name == "code" })?.value, !code.isEmpty else {
            throw GoogleOAuthError.missingAuthorizationCode
        }
        return code
    }

    /// 交换授权码为 Google token。
    ///
    /// - Parameters:
    ///   - code: 授权码
    ///   - codeVerifier: PKCE 校验码
    ///   - configuration: Google OAuth 配置
    /// - Returns: Google token 响应
    private func exchangeCode(
        _ code: String,
        codeVerifier: String,
        configuration: GoogleOAuthConfiguration
    ) async throws -> GoogleOAuthTokenResponse {
        var request = URLRequest(url: URL(string: "https://oauth2.googleapis.com/token")!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = formEncodedBody([
            "client_id": configuration.clientID,
            "code": code,
            "code_verifier": codeVerifier,
            "grant_type": "authorization_code",
            "redirect_uri": configuration.redirectURI
        ])
        let (data, response) = try await urlSession.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            throw GoogleOAuthError.tokenExchangeFailed
        }
        return try JSONDecoder().decode(GoogleOAuthTokenResponse.self, from: data)
    }

    /// 生成 form-urlencoded 请求体。
    ///
    /// - Parameter parameters: 请求参数
    /// - Returns: 请求体数据
    private func formEncodedBody(_ parameters: [String: String]) -> Data {
        let body = parameters
            .map { key, value in
                "\(formEscape(key))=\(formEscape(value))"
            }
            .joined(separator: "&")
        return Data(body.utf8)
    }

    /// 转义 form-urlencoded 值。
    ///
    /// - Parameter value: 原始值
    /// - Returns: 转义后的值
    private func formEscape(_ value: String) -> String {
        let allowed = CharacterSet(charactersIn: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }
}

/// Google OAuth 配置。
private struct GoogleOAuthConfiguration {
    /// iOS OAuth Client ID。
    let clientID: String
    /// 回调 URL Scheme。
    let redirectScheme: String

    /// 回调 URI。
    var redirectURI: String { "\(redirectScheme):/oauth2redirect/google" }

    /// 从 Info.plist 读取 Google OAuth 配置。
    ///
    /// - Parameter bundle: App Bundle
    /// - Returns: Google OAuth 配置
    static func load(from bundle: Bundle) throws -> GoogleOAuthConfiguration {
        let clientID = normalized(bundle.object(forInfoDictionaryKey: "GLOWUP_GOOGLE_IOS_CLIENT_ID") as? String)
        let redirectScheme = normalized(bundle.object(forInfoDictionaryKey: "GLOWUP_GOOGLE_REDIRECT_SCHEME") as? String)
        guard !clientID.isEmpty, !redirectScheme.isEmpty else {
            throw GoogleOAuthError.notConfigured
        }
        return GoogleOAuthConfiguration(clientID: clientID, redirectScheme: redirectScheme)
    }

    /// 标准化配置值。
    ///
    /// - Parameter value: 原始配置值
    /// - Returns: 标准化配置值
    private static func normalized(_ value: String?) -> String {
        let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.isEmpty || trimmed.hasPrefix("$(") || trimmed.contains("YOUR_") {
            return ""
        }
        return trimmed
    }
}

/// Google token 响应。
private struct GoogleOAuthTokenResponse: Decodable {
    /// Google OpenID Connect ID token。
    let idToken: String

    /// JSON 字段映射。
    private enum CodingKeys: String, CodingKey {
        /// Google ID token 字段。
        case idToken = "id_token"
    }
}

/// Google ID token payload。
private struct GoogleIDTokenPayload: Decodable {
    /// Google 用户唯一 ID。
    let sub: String
    /// Google 邮箱。
    let email: String?
    /// Google 展示名。
    let name: String?
    /// Google 名字。
    let givenName: String?

    /// 展示名。
    var displayName: String {
        if let name, !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return name
        }
        if let givenName, !givenName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return givenName
        }
        if let email, !email.isEmpty {
            return email.emailDisplayNameForGoogleOAuth
        }
        return "Google User"
    }

    /// JSON 字段映射。
    private enum CodingKeys: String, CodingKey {
        /// Google 用户唯一 ID。
        case sub
        /// Google 邮箱。
        case email
        /// Google 展示名。
        case name
        /// Google 名字。
        case givenName = "given_name"
    }

    /// 解码 Google ID token payload。
    ///
    /// - Parameter idToken: Google ID token
    /// - Returns: Google ID token payload
    static func decode(from idToken: String) throws -> GoogleIDTokenPayload {
        let parts = idToken.split(separator: ".")
        guard parts.count >= 2 else {
            throw GoogleOAuthError.invalidIDToken
        }
        var payload = String(parts[1])
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        let padding = (4 - payload.count % 4) % 4
        payload += String(repeating: "=", count: padding)
        guard let data = Data(base64Encoded: payload) else {
            throw GoogleOAuthError.invalidIDToken
        }
        return try JSONDecoder().decode(GoogleIDTokenPayload.self, from: data)
    }
}

/// PKCE 工具。
private enum PKCE {
    /// 允许的随机字符。
    private static let allowedCharacters = Array("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~")

    /// 生成随机字符串。
    ///
    /// - Parameter length: 字符串长度
    /// - Returns: 随机字符串
    static func randomString(length: Int) -> String {
        var bytes = [UInt8](repeating: 0, count: length)
        let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        if status != errSecSuccess {
            return String((0..<length).compactMap { _ in allowedCharacters.randomElement() })
        }
        return String(bytes.map { byte in
            allowedCharacters[Int(byte) % allowedCharacters.count]
        })
    }

    /// 生成 S256 code challenge。
    ///
    /// - Parameter verifier: PKCE verifier
    /// - Returns: code challenge
    static func codeChallenge(from verifier: String) -> String {
        let digest = SHA256.hash(data: Data(verifier.utf8))
        return Data(digest)
            .base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

/// Google OAuth 错误。
enum GoogleOAuthError: LocalizedError {
    /// 未配置 Google OAuth。
    case notConfigured
    /// 授权 URL 无效。
    case invalidAuthorizationURL
    /// 系统认证会话启动失败。
    case sessionStartFailed
    /// 缺少回调 URL。
    case missingCallbackURL
    /// Google 返回错误。
    case providerError(String)
    /// state 不匹配。
    case stateMismatch
    /// 缺少授权码。
    case missingAuthorizationCode
    /// token 交换失败。
    case tokenExchangeFailed
    /// ID token 无效。
    case invalidIDToken

    /// 错误说明。
    var errorDescription: String? {
        switch self {
        case .notConfigured:
            return "Google login is not configured. Set GLOWUP_GOOGLE_IOS_CLIENT_ID and GLOWUP_GOOGLE_REDIRECT_SCHEME in the iOS target build settings."
        case .invalidAuthorizationURL:
            return "Google authorization URL is invalid."
        case .sessionStartFailed:
            return "Google login could not be started."
        case .missingCallbackURL:
            return "Google login did not return a callback URL."
        case .providerError(let message):
            return "Google login failed: \(message)"
        case .stateMismatch:
            return "Google login state validation failed."
        case .missingAuthorizationCode:
            return "Google login did not return an authorization code."
        case .tokenExchangeFailed:
            return "Google token exchange failed."
        case .invalidIDToken:
            return "Google ID token is invalid."
        }
    }
}

private extension String {
    /// 从 Google OAuth 邮箱生成展示名。
    var emailDisplayNameForGoogleOAuth: String {
        let localPart = split(separator: "@").first.map(String.init) ?? self
        let cleaned = localPart
            .replacingOccurrences(of: ".", with: " ")
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
        return cleaned
            .split(separator: " ")
            .map { word in
                word.prefix(1).uppercased() + word.dropFirst()
            }
            .joined(separator: " ")
    }
}
