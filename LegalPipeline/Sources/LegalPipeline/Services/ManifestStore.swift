import Foundation
import SQLite

// Swift also declares `Expression` in Foundation; SQLite's version collides.
// Resolve the collision at point of use by qualifying `SQLite.Expression`
// inside the `cols` tuple below, rather than redeclaring the name.

// MARK: - Manifest Store

public class ManifestStore {
    private let db: Connection
    private let table: Table
    private let cols: (id: SQLite.Expression<String>, sourceURL: SQLite.Expression<String?>, sourceDomain: SQLite.Expression<String?>,
                       sourceType: SQLite.Expression<String>, court: SQLite.Expression<String?>, documentType: SQLite.Expression<String>,
                       sha256: SQLite.Expression<String>, sizeBytes: SQLite.Expression<Int64>, pageCount: SQLite.Expression<Int?>,
                       language: SQLite.Expression<String?>, retrievedAt: SQLite.Expression<Date>, license: SQLite.Expression<String?>,
                       status: SQLite.Expression<String>, ocrEngine: SQLite.Expression<String?>, ocrConfidence: SQLite.Expression<Double?>,
                       qualityScore: SQLite.Expression<Int?>, duplicateGroupID: SQLite.Expression<String?>,
                       canonicalDocumentID: SQLite.Expression<String?>, tier: SQLite.Expression<String?>,
                       normalizedTextPath: SQLite.Expression<String?>, failureStage: SQLite.Expression<String?>,
                       failureError: SQLite.Expression<String?>, retryCount: SQLite.Expression<Int>)

    public init(path: URL) throws {
        let dir = path.deletingLastPathComponent()
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        self.db = try Connection(path.path)
        self.db.busyTimeout = 5.0
        self.cols = (
            SQLite.Expression<String>("document_id"),
            SQLite.Expression<String?>("source_url"),
            SQLite.Expression<String?>("source_domain"),
            SQLite.Expression<String>("source_type"),
            SQLite.Expression<String?>("court"),
            SQLite.Expression<String>("document_type"),
            SQLite.Expression<String>("sha256"),
            SQLite.Expression<Int64>("size_bytes"),
            SQLite.Expression<Int?>("page_count"),
            SQLite.Expression<String?>("language"),
            SQLite.Expression<Date>("retrieved_at"),
            SQLite.Expression<String?>("license"),
            SQLite.Expression<String>("processing_status"),
            SQLite.Expression<String?>("ocr_engine"),
            SQLite.Expression<Double?>("ocr_confidence"),
            SQLite.Expression<Int?>("quality_score"),
            SQLite.Expression<String?>("duplicate_group_id"),
            SQLite.Expression<String?>("canonical_document_id"),
            SQLite.Expression<String?>("tier"),
            SQLite.Expression<String?>("normalized_text_path"),
            SQLite.Expression<String?>("failure_stage"),
            SQLite.Expression<String?>("failure_error"),
            SQLite.Expression<Int>("retry_count")
         )
        self.table = Table("manifests")
        try createTable()
    }

    private func createTable() throws {
        try db.run(table.create(ifNotExists: true) { t in
            t.column(cols.id, primaryKey: true)
            t.column(cols.sourceURL)
            t.column(cols.sourceDomain)
            t.column(cols.sourceType)
            t.column(cols.court)
            t.column(cols.documentType)
            t.column(cols.sha256, unique: true)
            t.column(cols.sizeBytes)
            t.column(cols.pageCount)
            t.column(cols.language)
            t.column(cols.retrievedAt)
            t.column(cols.license)
            t.column(cols.status)
            t.column(cols.ocrEngine)
            t.column(cols.ocrConfidence)
            t.column(cols.qualityScore)
            t.column(cols.duplicateGroupID)
            t.column(cols.canonicalDocumentID)
            t.column(cols.tier)
            t.column(cols.normalizedTextPath)
            t.column(cols.failureStage)
            t.column(cols.failureError)
            t.column(cols.retryCount, defaultValue: 0)
        })
    }

    public func insertOrReplace(_ doc: DocumentManifest) throws {
        let insert = table.insert(or: .replace,
            cols.id <- doc.documentID,
            cols.sourceURL <- doc.sourceURL,
            cols.sourceDomain <- doc.sourceDomain,
            cols.sourceType <- doc.sourceType.rawValue,
            cols.court <- doc.court,
            cols.documentType <- doc.documentType.rawValue,
            cols.sha256 <- doc.sha256,
            cols.sizeBytes <- doc.sizeBytes,
            cols.pageCount <- doc.pageCount,
            cols.language <- doc.language,
            cols.retrievedAt <- doc.retrievedAt,
            cols.license <- doc.license,
            cols.status <- doc.processingStatus.rawValue,
            cols.ocrEngine <- doc.ocrEngine,
            cols.ocrConfidence <- doc.ocrConfidence,
            cols.qualityScore <- doc.qualityScore,
            cols.duplicateGroupID <- doc.duplicateGroupID,
            cols.canonicalDocumentID <- doc.canonicalDocumentID,
            cols.tier <- doc.tier?.rawValue,
            cols.normalizedTextPath <- doc.normalizedTextPath,
            cols.failureStage <- doc.failureStage,
            cols.failureError <- doc.failureError,
            cols.retryCount <- doc.retryCount
        )
        try db.run(insert)
    }

    public func get(id: String) throws -> DocumentManifest? {
        guard let row = try db.pluck(table.filter(cols.id == id)) else { return nil }
        return rowToManifest(row)
    }

    public func findBySHA(sha: String) throws -> DocumentManifest? {
        guard let row = try db.pluck(table.filter(cols.sha256 == sha)) else { return nil }
        return rowToManifest(row)
    }

    public func listByStatus(_ status: ProcessingStatus) throws -> [DocumentManifest] {
        let query = table.filter(cols.status == status.rawValue)
        return try db.prepare(query).map { rowToManifest($0) }
    }

    public func listPending() throws -> [DocumentManifest] {
        return try listByStatus(.pending)
    }

    public func listFailed() throws -> [DocumentManifest] {
        return try listByStatus(.failed)
    }

    public func countByStatus() throws -> [ProcessingStatus: Int] {
        var counts: [ProcessingStatus: Int] = [:]
        for status in ProcessingStatus.allCases {
            counts[status] = try db.scalar(table.filter(cols.status == status.rawValue).count)
        }
        return counts
    }

    public func totalCount() throws -> Int {
        return try db.scalar(table.count)
    }

    public func updateStatus(id: String, status: ProcessingStatus) throws {
        let row = table.filter(cols.id == id)
        try db.run(row.update(
            cols.status <- status.rawValue,
            cols.retryCount <- 0,
            cols.failureStage <- nil,
            cols.failureError <- nil
        ))
    }

    public func markNormalized(id: String, path: String) throws {
        let row = table.filter(cols.id == id)
        try db.run(row.update(
            cols.status <- ProcessingStatus.normalized.rawValue,
            cols.normalizedTextPath <- path
        ))
    }

    public func markOCRRequired(id: String, engine: String) throws {
        let row = table.filter(cols.id == id)
        try db.run(row.update(
            cols.status <- ProcessingStatus.ocr.rawValue,
            cols.ocrEngine <- engine
        ))
    }

    public func checkpoint() throws -> String? {
        guard let row = try db.pluck(table
            .select(cols.id)
            .filter(cols.status != ProcessingStatus.pending.rawValue && cols.status != ProcessingStatus.failed.rawValue)
            .order(cols.retrievedAt.desc)
            .limit(1)) else { return nil }
        return row[cols.id]
    }

    private func rowToManifest(_ row: Row) -> DocumentManifest {
        DocumentManifest(
            documentID: row[cols.id],
            sourceURL: row[cols.sourceURL],
            sourceDomain: row[cols.sourceDomain],
            sourceType: SourceType(rawValue: row[cols.sourceType]) ?? .other,
            court: row[cols.court],
            documentType: DocumentType(rawValue: row[cols.documentType]) ?? .other,
            sha256: row[cols.sha256],
            sizeBytes: row[cols.sizeBytes],
            pageCount: row[cols.pageCount],
            language: row[cols.language],
            retrievedAt: row[cols.retrievedAt],
            license: row[cols.license],
            processingStatus: ProcessingStatus(rawValue: row[cols.status]) ?? .pending,
            ocrEngine: row[cols.ocrEngine],
            ocrConfidence: row[cols.ocrConfidence],
            qualityScore: row[cols.qualityScore],
            duplicateGroupID: row[cols.duplicateGroupID],
            canonicalDocumentID: row[cols.canonicalDocumentID],
            tier: row[cols.tier].flatMap { DocumentTier(rawValue: $0) }
        )
    }
}
