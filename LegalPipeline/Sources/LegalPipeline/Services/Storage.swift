import Foundation
import CryptoKit

// MARK: - Storage Paths

public struct StoragePaths {
    public let storageRoot: URL

    public init(storageRoot: URL) {
        self.storageRoot = storageRoot
    }

    public func rawPath(for documentID: String, sourceType: SourceType) -> URL {
        let extMap: [SourceType: String] = [
            .pdf: "pdf", .html: "html", .image: "png", .docx: "docx",
            .txt: "txt", .xml: "xml", .json: "json", .csv: "csv",
            .epub: "epub", .other: "bin"
        ]
        let ext = extMap[sourceType] ?? "bin"
        return storageRoot
            .appendingPathComponent("raw/\(sourceType.rawValue)s")
            .appendingPathComponent("\(documentID).\(ext)")
    }

    public func processedTextPath(for documentID: String, page: Int? = nil) -> URL {
        let dir = storageRoot.appendingPathComponent("processed/text/\(documentID)")
        if let page = page {
            return dir.appendingPathComponent(String(format: "page-%04d.txt", page))
        }
        return dir.appendingPathComponent("normalized.txt")
    }

    public func processedDir(for documentID: String, stage: String) -> URL {
        return storageRoot.appendingPathComponent("processed/\(stage)/\(documentID)")
    }

    public func ensureDirectories(for documentID: String) throws {
        let dirs = [
            storageRoot.appendingPathComponent("raw/source"),
            storageRoot.appendingPathComponent("raw/pdf"),
            storageRoot.appendingPathComponent("raw/html"),
            storageRoot.appendingPathComponent("raw/images"),
            storageRoot.appendingPathComponent("processed/text/\(documentID)"),
        ]
        for d in dirs {
            try FileManager.default.createDirectory(at: d, withIntermediateDirectories: true)
        }
    }
}

// MARK: - SHA-256

public func computeSHA256(fileAt url: URL) throws -> String {
    let data = try Data(contentsOf: url)
    let hash = SHA256.hash(data: data)
    return hash.map { String(format: "%02x", $0) }.joined()
}

// MARK: - Stage Raw File

public struct StagedFile {
    public let destinationURL: URL
    public let sha256: String
    public let sizeBytes: Int64
}

public func stageRawFile(
    sourceURL: URL,
    documentID: String,
    sourceType: SourceType,
    storage: StoragePaths
) throws -> StagedFile {
    let dest = storage.rawPath(for: documentID, sourceType: sourceType)
    try storage.ensureDirectories(for: documentID)
    try FileManager.default.copyItem(at: sourceURL, to: dest)
    let sha = try computeSHA256(fileAt: dest)
    let attrs = try FileManager.default.attributesOfItem(atPath: dest.path)
    let size = attrs[.size] as? Int64 ?? 0
    return StagedFile(destinationURL: dest, sha256: sha, sizeBytes: size)
}
