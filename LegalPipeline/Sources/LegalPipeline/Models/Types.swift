import Foundation

// MARK: - Models

public enum ProcessingStatus: String, Codable, CaseIterable {
    case pending
    case downloaded
    case validated
    case parsed
    case ocr
    case normalized
    case metadataComplete = "metadata_complete"
    case citationComplete = "citation_complete"
    case deduplicated
    case qualityChecked = "quality_checked"
    case exported
    case failed
    case needsReview = "needs_review"
}

public enum SourceType: String, Codable, CaseIterable {
    case pdf
    case html
    case image
    case docx
    case txt
    case xml
    case json
    case csv
    case epub
    case other
}

public enum DocumentType: String, Codable, CaseIterable {
    case judgment
    case act
    case rule
    case regulation
    case notification
    case circular
    case govOrder = "go"
    case legislative
    case parliamentary
    case commentary
    case textbook
    case journal
    case contract
    case procedural
    case dictionary
    case faq
    case website
    case other
}

public enum DocumentTier: String, Codable, CaseIterable {
    case a = "A"
    case b = "B"
    case c = "C"
    case d = "D"
}
