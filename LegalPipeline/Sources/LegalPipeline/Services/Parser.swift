import Foundation
import PDFKit
import SwiftSoup

// MARK: - Parse Result

public struct ParseResult {
    public let documentID: String
    public let totalPages: Int
    public let nativeExtraction: Bool
    public let ocrRequired: Bool
    public let textPerPage: [PageText]
    public let error: String?

    public init(
        documentID: String,
        totalPages: Int,
        nativeExtraction: Bool,
        ocrRequired: Bool,
        textPerPage: [PageText],
        error: String? = nil
    ) {
        self.documentID = documentID
        self.totalPages = totalPages
        self.nativeExtraction = nativeExtraction
        self.ocrRequired = ocrRequired
        self.textPerPage = textPerPage
        self.error = error
    }
}

public struct PageText: Codable {
    public let pageNumber: Int
    public let text: String
    public let hasTextLayer: Bool
    public let pageSize: CGSize

    public init(pageNumber: Int, text: String, hasTextLayer: Bool, pageSize: CGSize) {
        self.pageNumber = pageNumber
        self.text = text
        self.hasTextLayer = hasTextLayer
        self.pageSize = pageSize
    }
}

// MARK: - Parser

public class DocumentParser {
    private let storage: StoragePaths
    public let ocrThresholdCharsPerPage: Int
    public let ocrThresholdPagesFraction: Double

    public init(
        storage: StoragePaths,
        ocrThresholdCharsPerPage: Int = 50,
        ocrThresholdPagesFraction: Double = 0.5
    ) {
        self.storage = storage
        self.ocrThresholdCharsPerPage = ocrThresholdCharsPerPage
        self.ocrThresholdPagesFraction = ocrThresholdPagesFraction
    }

    public func parse(documentID: String, sourceURL: URL, sourceType: SourceType) throws -> ParseResult {
        switch sourceType {
        case .pdf:
            return try parsePDF(documentID: documentID, sourceURL: sourceURL)
        case .txt:
            return try parseTXT(documentID: documentID, sourceURL: sourceURL)
        case .html:
            return try parseHTML(documentID: documentID, sourceURL: sourceURL)
        case .docx:
            return try parseDOCX(documentID: documentID, sourceURL: sourceURL)
        default:
            throw PipelineError.unsupportedFormat(sourceType)
        }
    }

    // MARK: - PDF

    private func parsePDF(documentID: String, sourceURL: URL) throws -> ParseResult {
        let document = PDFDocument(url: sourceURL)
        guard let pdf = document else {
            throw PipelineError.pdfOpenFailed(documentID)
        }
        let pageCount = pdf.pageCount
        var pageTexts: [PageText] = []
        var totalChars = 0
        var pagesWithText = 0

        for i in 0..<pageCount {
            guard let page = pdf.page(at: i) else {
                pageTexts.append(PageText(pageNumber: i + 1, text: "", hasTextLayer: false, pageSize: .zero))
                continue
            }
            let pageRect = page.bounds(for: .mediaBox)
            let text = page.string ?? ""
            let hasTextLayer = !text.isEmpty
            if hasTextLayer {
                totalChars += text.count
                pagesWithText += 1
            }
            pageTexts.append(PageText(pageNumber: i + 1, text: text, hasTextLayer: hasTextLayer, pageSize: pageRect.size))
        }

        let avgCharsPerPage = pageCount > 0 ? Double(totalChars) / Double(pageCount) : 0
        let fractionWithText = pageCount > 0 ? Double(pagesWithText) / Double(pageCount) : 0
        let ocrRequired = avgCharsPerPage < Double(ocrThresholdCharsPerPage) && fractionWithText < ocrThresholdPagesFraction

        return ParseResult(
            documentID: documentID,
            totalPages: pageCount,
            nativeExtraction: true,
            ocrRequired: ocrRequired,
            textPerPage: pageTexts
        )
    }

    // MARK: - TXT

    private func parseTXT(documentID: String, sourceURL: URL) throws -> ParseResult {
        let text = try String(contentsOf: sourceURL, encoding: .utf8)
        let pageTexts = [PageText(pageNumber: 1, text: text, hasTextLayer: true, pageSize: .zero)]
        return ParseResult(
            documentID: documentID,
            totalPages: 1,
            nativeExtraction: true,
            ocrRequired: false,
            textPerPage: pageTexts
        )
    }

    // MARK: - HTML

    private func parseHTML(documentID: String, sourceURL: URL) throws -> ParseResult {
        let html = try String(contentsOf: sourceURL, encoding: .utf8)
        let doc = try SwiftSoup.parse(html)
        let text = try doc.body()?.text() ?? ""
        let pageTexts = [PageText(pageNumber: 1, text: text, hasTextLayer: true, pageSize: .zero)]
        return ParseResult(
            documentID: documentID,
            totalPages: 1,
            nativeExtraction: true,
            ocrRequired: false,
            textPerPage: pageTexts
        )
    }

    // MARK: - DOCX

    private func parseDOCX(documentID: String, sourceURL: URL) throws -> ParseResult {
        // DOCX is a zip; for now we flag as needing external extraction
        // In production, spawn a Python script with python-docx or use a Swift unzip + XML parse
        throw PipelineError.docxRequiresExternalTool(documentID)
    }
}

// MARK: - Errors

public enum PipelineError: Error, LocalizedError {
    case unsupportedFormat(SourceType)
    case pdfOpenFailed(String)
    case docxRequiresExternalTool(String)
    case fileNotFound(String)
    case parseFailed(String)

    public var errorDescription: String? {
        switch self {
        case .unsupportedFormat(let t): return "Unsupported source type: \(t.rawValue)"
        case .pdfOpenFailed(let id): return "Failed to open PDF for \(id)"
        case .docxRequiresExternalTool(let id): return "DOCX requires external extraction tool for \(id)"
        case .fileNotFound(let path): return "File not found: \(path)"
        case .parseFailed(let msg): return "Parse failed: \(msg)"
        }
    }
}
