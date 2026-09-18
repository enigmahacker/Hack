import Foundation

// MARK: - Document Manifest

public struct DocumentManifest: Codable, Identifiable {
    public var id: String { documentID }
    public let documentID: String
    public let sourceURL: String?
    public let sourceDomain: String?
    public let sourceType: SourceType
    public let court: String?
    public let documentType: DocumentType
    public let sha256: String
    public let sizeBytes: Int64
    public var pageCount: Int?
    public var language: String?
    public let retrievedAt: Date
    public let license: String?
    public var processingStatus: ProcessingStatus
    public var ocrEngine: String?
    public var ocrConfidence: Double?
    public var qualityScore: Int?
    public var duplicateGroupID: String?
    public var canonicalDocumentID: String?
    public var tier: DocumentTier?
    public var normalizedTextPath: String? = nil
    public var failureStage: String? = nil
    public var failureError: String? = nil
    public var retryCount: Int = 0

    public init(
        documentID: String,
        sourceURL: String? = nil,
        sourceDomain: String? = nil,
        sourceType: SourceType,
        court: String? = nil,
        documentType: DocumentType,
        sha256: String,
        sizeBytes: Int64,
        pageCount: Int? = nil,
        language: String? = nil,
        retrievedAt: Date = Date(),
        license: String? = nil,
        processingStatus: ProcessingStatus = .pending,
        ocrEngine: String? = nil,
        ocrConfidence: Double? = nil,
        qualityScore: Int? = nil,
        duplicateGroupID: String? = nil,
        canonicalDocumentID: String? = nil,
        tier: DocumentTier? = nil,
        normalizedTextPath: String? = nil,
        failureStage: String? = nil,
        failureError: String? = nil,
        retryCount: Int = 0
    ) {
        self.documentID = documentID
        self.sourceURL = sourceURL
        self.sourceDomain = sourceDomain
        self.sourceType = sourceType
        self.court = court
        self.documentType = documentType
        self.sha256 = sha256
        self.sizeBytes = sizeBytes
        self.pageCount = pageCount
        self.language = language
        self.retrievedAt = retrievedAt
        self.license = license
        self.processingStatus = processingStatus
        self.ocrEngine = ocrEngine
        self.ocrConfidence = ocrConfidence
        self.qualityScore = qualityScore
        self.duplicateGroupID = duplicateGroupID
        self.canonicalDocumentID = canonicalDocumentID
        self.tier = tier
        self.normalizedTextPath = normalizedTextPath
        self.failureStage = failureStage
        self.failureError = failureError
        self.retryCount = retryCount
     }
}
