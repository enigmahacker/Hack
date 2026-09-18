import Foundation

// MARK: - Pipeline Configuration

public struct PipelineConfig {
    public let storageRoot: URL
    public let manifestDBPath: URL
    public let targetTokens: Int64
    public let languageAllocation: [String: Double]

    public init(
        storageRoot: URL? = nil,
        manifestDBPath: URL? = nil,
        targetTokens: Int64 = 1_000_000_000_000,
        languageAllocation: [String: Double] = [
            "en": 0.60,
            "hi": 0.15,
            "other_indian": 0.10,
            "legal_docs": 0.10,
            "synthetic_sft": 0.05
        ]
    ) {
        let cwd: URL = {
            var buf = [Int8](repeating: 0, count: Int(PATH_MAX))
            guard getcwd(&buf, Int(PATH_MAX)) != nil else {
                return FileManager.default.homeDirectoryForCurrentUser
            }
            return URL(fileURLWithPath: String(cString: buf))
        }()
        self.storageRoot = storageRoot ?? cwd.appendingPathComponent("legal-corpus")
        let root = self.storageRoot
        self.manifestDBPath = manifestDBPath ?? root.appendingPathComponent("manifests/manifest.sqlite")
        self.targetTokens = targetTokens
        self.languageAllocation = languageAllocation
    }

    public mutating func ensureDirectories() throws {
        let subdirs = [
            "raw/source", "raw/pdf", "raw/html", "raw/images",
            "processed/text", "processed/pages", "processed/paragraphs",
            "processed/metadata", "processed/citations", "processed/entities",
            "processed/relationships",
            "training/pretrain", "training/sft", "training/preference",
            "embeddings", "indexes", "manifests", "qa"
        ]
        for sub in subdirs {
            try FileManager.default.createDirectory(
                at: storageRoot.appendingPathComponent(sub),
                withIntermediateDirectories: true
            )
        }
    }
}
