import Foundation
import UIKit
import UniformTypeIdentifiers

/// 照片导入元数据。
struct PhotoImportDescriptor {
    /// 文件扩展名。
    let fileExtension: String
    /// MIME 类型。
    let mimeType: String
}

/// 照片导入结果。
struct PhotoImportPayload {
    /// 准备上传的照片数据。
    let data: Data
    /// 上传文件元数据。
    let descriptor: PhotoImportDescriptor
}

/// 照片导入辅助工具。
enum PhotoImportSupport {
    /// 准备上传照片数据。
    ///
    /// HEIC/HEIF 是 iOS 用户常见格式，但多数 Vision API 不直接接受。这里在客户端转成 JPEG，
    /// 既保留用户选择 HEIC 的能力，也让后端 AI provider 拿到兼容格式。
    ///
    /// - Parameters:
    ///   - data: 原始照片数据
    ///   - supportedContentTypes: 系统照片支持的内容类型
    /// - Returns: 可上传照片数据和元数据
    static func payload(from data: Data, supportedContentTypes: [UTType]) throws -> PhotoImportPayload {
        let contentType = preferredImageContentType(from: supportedContentTypes)
        if isHeicOrHeif(contentType) || isHeicOrHeifData(data) {
            guard let image = UIImage(data: data),
                  let jpegData = image.jpegData(compressionQuality: 0.9) else {
                throw PhotoImportError.conversionFailed
            }
            return PhotoImportPayload(
                data: jpegData,
                descriptor: PhotoImportDescriptor(fileExtension: "jpg", mimeType: "image/jpeg")
            )
        }
        return PhotoImportPayload(data: data, descriptor: descriptor(for: supportedContentTypes))
    }

    /// 根据系统照片内容类型生成导入元数据。
    ///
    /// - Parameter supportedContentTypes: 系统照片支持的内容类型
    /// - Returns: 照片导入元数据
    static func descriptor(for supportedContentTypes: [UTType]) -> PhotoImportDescriptor {
        let contentType = preferredImageContentType(from: supportedContentTypes)
        return PhotoImportDescriptor(
            fileExtension: fileExtension(for: contentType),
            mimeType: mimeType(for: contentType)
        )
    }

    /// 判断内容类型是否是 HEIC 或 HEIF。
    ///
    /// - Parameter contentType: 图片内容类型
    /// - Returns: 是否为 HEIC/HEIF
    private static func isHeicOrHeif(_ contentType: UTType) -> Bool {
        let identifier = contentType.identifier.lowercased()
        return identifier.contains("heic") || identifier.contains("heif")
    }

    /// 通过文件头判断数据是否为 HEIC/HEIF。
    ///
    /// - Parameter data: 原始图片数据
    /// - Returns: 是否为 HEIC/HEIF 数据
    private static func isHeicOrHeifData(_ data: Data) -> Bool {
        guard data.count >= 12 else {
            return false
        }
        let boxTypeRange = data.index(data.startIndex, offsetBy: 4)..<data.index(data.startIndex, offsetBy: 8)
        let brandRange = data.index(data.startIndex, offsetBy: 8)..<data.index(data.startIndex, offsetBy: 12)
        guard String(data: Data(data[boxTypeRange]), encoding: .ascii) == "ftyp",
              let brand = String(data: Data(data[brandRange]), encoding: .ascii)?.lowercased() else {
            return false
        }
        return ["heic", "heix", "hevc", "hevx", "mif1", "msf1"].contains(brand)
    }

    /// 从内容类型列表中推断优先图片类型。
    ///
    /// - Parameter supportedContentTypes: 系统照片支持的内容类型
    /// - Returns: 优先图片类型
    private static func preferredImageContentType(from supportedContentTypes: [UTType]) -> UTType {
        supportedContentTypes.first { contentType in
            contentType.conforms(to: .jpeg)
                || contentType.conforms(to: .png)
                || contentType.identifier.lowercased().contains("heic")
                || contentType.identifier.lowercased().contains("heif")
        } ?? .jpeg
    }

    /// 获取图片扩展名。
    ///
    /// - Parameter contentType: 图片内容类型
    /// - Returns: 文件扩展名
    private static func fileExtension(for contentType: UTType) -> String {
        if contentType.conforms(to: .png) {
            return "png"
        }
        if contentType.identifier.lowercased().contains("heic") {
            return "heic"
        }
        if contentType.identifier.lowercased().contains("heif") {
            return "heif"
        }
        return "jpg"
    }

    /// 获取图片 MIME 类型。
    ///
    /// - Parameter contentType: 图片内容类型
    /// - Returns: MIME 类型
    private static func mimeType(for contentType: UTType) -> String {
        if contentType.conforms(to: .png) {
            return "image/png"
        }
        if contentType.identifier.lowercased().contains("heic") {
            return "image/heic"
        }
        if contentType.identifier.lowercased().contains("heif") {
            return "image/heif"
        }
        return "image/jpeg"
    }
}

/// 照片导入错误。
enum PhotoImportError: LocalizedError {
    /// HEIC/HEIF 转 JPEG 失败。
    case conversionFailed

    /// 错误说明。
    var errorDescription: String? {
        switch self {
        case .conversionFailed:
            return "Photo could not be converted for upload."
        }
    }
}
