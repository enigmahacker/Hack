import ArgumentParser
import Foundation

// MARK: - Main CLI Entry Point

struct LegalCLI: ParsableCommand {
    static var configuration = CommandConfiguration(
        commandName: "legal",
        abstract: "Indian Legal AI Dataset Pipeline CLI",
        subcommands: [Ingest.self, Parse.self, Normalize.self, Stats.self, Resume.self,
                      Ocr.self, Metadata.self, Citations.self, Dedupe.self, Quality.self,
                      Tokenize.self, BuildDataset.self, IndexCommand.self],
     )
}

// MARK: - Ingest

struct Ingest: ParsableCommand {
    @Argument(help: "Path to JSONL manifest of source documents")
    var source: String

    @Option(name: [.short, .long], help: "Number of parallel workers")
    var workers: Int = 1

    @Flag(name: .shortAndLong, help: "Verbose logging")
    var verbose: Bool = false

    func run() throws {
        let config = try PipelineConfig()
        try config.ensureDirectories()
        let storage = StoragePaths(storageRoot: config.storageRoot)
        let manifestDB = try ManifestStore(path: config.manifestDB)
        let generator = DocumentIDGenerator()

        let entries = try parseJSONL(source)
        var ingested = 0
        var skipped = 0

        for entry in entries {
            if let existing = try manifestDB.findBySHA(sha: entry.sha256) {
                skipped += 1
                continue
            }
            let docID = generator.next()
            let staged = try stageRawFile(
                sourceURL: URL(fileURLWithPath: entry.filePath),
                documentID: docID,
                sourceType: entry.sourceType,
                storage: storage
            )
            let doc = DocumentManifest(
                documentID: docID,
                sourceURL: entry.sourceURL,
                sourceDomain: entry.sourceDomain,
                sourceType: entry.sourceType,
                court: entry.court,
                documentType: entry.documentType,
                sha256: staged.sha256,
                sizeBytes: staged.sizeBytes,
                pageCount: entry.pageCount,
                language: entry.language,
                license: entry.license,
                processingStatus: .downloaded
            )
            try manifestDB.insertOrReplace(doc)
            ingested += 1
            if verbose {
                print("[ingest] \(docID) -> \(staged.destinationURL.lastPathComponent) (\(staged.sizeBytes) bytes)")
            }
        }
        print("Ingest complete: \(ingested) ingested, \(skipped) skipped (by SHA).")
        print("Total manifests: \(try manifestDB.totalCount())")
    }

    private func parseJSONL(_ path: String) throws -> [IngestEntry] {
        let url = URL(fileURLWithPath: path)
        let content = try String(contentsOf: url, encoding: .utf8)
        return content.components(separatedBy: "\n")
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .compactMap { line -> IngestEntry? in
                guard let data = line.data(using: .utf8),
                      let entry = try? JSONDecoder().decode(IngestEntry.self, from: data) else {
                    return nil
                }
                return entry
            }
    }
}

struct IngestEntry: Codable {
    let filePath: String
    let sourceURL: String?
    let sourceDomain: String?
    let sourceType: SourceType
    let court: String?
    let documentType: DocumentType
    let sha256: String
    let pageCount: Int?
    let language: String?
    let license: String?
}

// MARK: - Parse

struct Parse: ParsableCommand {
    @Argument(help: "Document ID to parse, or --all-pending")
    var documentID: String?

    @Flag(name: .long, help: "Parse all pending documents")
    var allPending = false

    @Option(name: [.short, .long])
    var workers: Int = 1

    func run() throws {
        let config = try PipelineConfig()
        let storage = StoragePaths(storageRoot: config.storageRoot)
        let manifestDB = try ManifestStore(path: config.manifestDB)
        let parser = DocumentParser(storage: storage)

        if allPending {
            let pending = try manifestDB.listPending()
            print("Parsing \(pending.count) pending documents...")
            for doc in pending {
                try processParse(doc: doc, parser: parser, manifestDB: manifestDB, storage: storage)
            }
            print("Parse complete.")
        } else if let id = documentID {
            guard let doc = try manifestDB.get(id: id) else {
                print("Document not found: \(id)")
                return
            }
            try processParse(doc: doc, parser: parser, manifestDB: manifestDB, storage: storage)
            print("Parsed \(id).")
        } else {
            print("Usage: legal parse <document-id> | legal parse --all-pending")
        }
    }

    private func processParse(
        doc: DocumentManifest,
        parser: DocumentParser,
        manifestDB: ManifestStore,
        storage: StoragePaths
    ) throws {
        let rawURL = storage.rawPath(for: doc.documentID, sourceType: doc.sourceType)
        do {
            let result = try parser.parse(
                documentID: doc.documentID,
                sourceURL: rawURL,
                sourceType: doc.sourceType
            )
            // Write page texts
            let pageDir = storage.processedDir(for: doc.documentID, stage: "text")
            try FileManager.default.createDirectory(at: pageDir, withIntermediateDirectories: true)
            for page in result.textPerPage {
                let pageURL = storage.processedTextPath(for: doc.documentID, page: page.pageNumber)
                try page.text.write(to: pageURL, atomically: true, encoding: .utf8)
            }
            if result.ocrRequired {
                try manifestDB.markOCRRequired(id: doc.documentID, engine: "tesseract")
                print("[parse] \(doc.documentID): OCR required (\(result.totalPages) pages)")
            } else {
                var updated = doc
                updated.processingStatus = .parsed
                updated.pageCount = result.totalPages
                try manifestDB.insertOrReplace(updated)
                print("[parse] \(doc.documentID): parsed \(result.totalPages) pages, native")
            }
        } catch {
            try manifestDB.insertOrReplace(DocumentManifest(
                documentID: doc.documentID,
                sourceURL: doc.sourceURL,
                sourceDomain: doc.sourceDomain,
                sourceType: doc.sourceType,
                court: doc.court,
                documentType: doc.documentType,
                sha256: doc.sha256,
                sizeBytes: doc.sizeBytes,
                pageCount: doc.pageCount,
                language: doc.language,
                retrievedAt: doc.retrievedAt,
                license: doc.license,
                processingStatus: .failed
            ))
            throw error
        }
    }
}

// MARK: - Normalize

struct Normalize: ParsableCommand {
    @Argument(help: "Document ID to normalize, or --all-parsed")
    var documentID: String?

    @Flag(name: .long, help: "Normalize all parsed documents")
    var allParsed = false

    func run() throws {
        let config = try PipelineConfig()
        let storage = StoragePaths(storageRoot: config.storageRoot)
        let manifestDB = try ManifestStore(path: config.manifestDB)

        if allParsed {
            let parsed = try manifestDB.listByStatus(.parsed)
            print("Normalizing \(parsed.count) parsed documents...")
            for doc in parsed {
                try processNormalize(doc: doc, manifestDB: manifestDB, storage: storage)
            }
            print("Normalize complete.")
        } else if let id = documentID {
            guard let doc = try manifestDB.get(id: id) else {
                print("Document not found: \(id)")
                return
            }
            try processNormalize(doc: doc, manifestDB: manifestDB, storage: storage)
            print("Normalized \(id).")
        } else {
            print("Usage: legal normalize <document-id> | legal normalize --all-parsed")
        }
    }

    private func processNormalize(doc: DocumentManifest, manifestDB: ManifestStore, storage: StoragePaths) throws {
        // Concatenate pages + normalize whitespace
        let pageDir = storage.processedDir(for: doc.documentID, stage: "text")
        var allText = ""
        let pageRegex = try NSRegularExpression(pattern: "page-\\d{4}\\.txt$", options: [])
        if let enumerator = FileManager.default.enumerator(
            at: pageDir,
            includingPropertiesForKeys: [.isRegularFileKey],
            options: [.skipsHiddenFiles]
        ) {
            for case let fileURL as URL in enumerator {
                if pageRegex.firstMatch(in: fileURL.lastPathComponent, options: [], range: NSRange(0..<fileURL.lastPathComponent.utf16.count)) != nil {
                    if let text = try? String(contentsOf: fileURL, encoding: .utf8) {
                        allText += text + "\n\n"
                    }
                }
            }
        }
        // Normalize whitespace
        let normalized = allText
            .replacingOccurrences(of: "\r\n", with: "\n")
            .replacingOccurrences(of: "\r", with: "\n")
            .split(separator: "\n", omittingEmptySubsequences: false)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .joined(separator: "\n")

        let normalizedURL = storage.processedTextPath(for: doc.documentID)
        try normalized.write(to: normalizedURL, atomically: true, encoding: .utf8)
        try manifestDB.markNormalized(id: doc.documentID, path: normalizedURL.path)
        print("[normalize] \(doc.documentID): \(normalized.count) chars -> \(normalizedURL.lastPathComponent)")
    }
}

// MARK: - Stats

struct Stats: ParsableCommand {
    func run() throws {
        let config = try PipelineConfig()
        let manifestDB = try ManifestStore(path: config.manifestDB)
        let counts = try manifestDB.countByStatus()
        let total = try manifestDB.totalCount()

        print("=== Pipeline Stats ===")
        print("Total manifests: \(total)")
        print("")
        print("By status:")
        for (status, count) in counts.sorted(by: { $0.key.rawValue < $1.key.rawValue }) {
            print("  \(status.rawValue): \(count)")
        }
    }
}

// MARK: - Resume

struct Resume: ParsableCommand {
    func run() throws {
        let config = try PipelineConfig()
        let manifestDB = try ManifestStore(path: config.manifestDB)
        let checkpoint = try manifestDB.checkpoint()
        let counts = try manifestDB.countByStatus()

        print("=== Resume ===")
        if let cp = checkpoint {
            print("Checkpoint (last processed): \(cp)")
        } else {
            print("No checkpoint — nothing processed yet.")
        }
        print("")
        print("Pending: \(counts[.pending] ?? 0)")
        print("Failed: \(counts[.failed] ?? 0)")
        print("Needs review: \(counts[.needsReview] ?? 0)")
        print("")
        print("To resume processing: legal parse --all-pending")
    }
}

// MARK: - Stub Commands

struct Ocr: ParsableCommand {
    func run() throws {
        print("OCR stage: not yet implemented")
        print("See spec section 7-8: PDF detection → text-layer detection → native extraction → OCR if required")
        print("When ready: spawn tesseract/paddleocr/doctr as external processes per document flagged .ocr status")
    }
}

struct Metadata: ParsableCommand {
    func run() throws {
        print("Metadata extraction: not yet implemented")
        print("See spec section 9: court, bench, case_name, case_number, citation, neutral_citation, judgment_date, judges, parties, statutes, sections, disposition...")
        print("Will implement regex/heuristic extraction first, then ML model")
    }
}

struct Citations: ParsableCommand {
    func run() throws {
        print("Citation extraction: not yet implemented")
        print("See spec section 10-11: AIR, SCC, SCC OnLine, SCR, Cri LJ, All LJ, ILR, etc. + relationship model")
    }
}

struct Dedupe: ParsableCommand {
    func run() throws {
        print("Deduplication: not yet implemented")
        print("See spec section 13: SHA-256 exact → normalized text hash → MinHash/SimHash near-dedup")
    }
}

struct Quality: ParsableCommand {
    func run() throws {
        print("Quality scoring: not yet implemented")
        print("See spec section 18: text_quality, ocr_quality, metadata_quality, citation_quality, language_quality, duplication_probability, source_reliability, legal_relevance")
    }
}

struct Tokenize: ParsableCommand {
    func run() throws {
        print("Tokenization: not yet implemented")
        print("See spec section 16-17: run actual training tokenizer, track token budget by language/court/document_type/source/year")
    }
}

struct BuildDataset: ParsableCommand {
    func run() throws {
        print("Dataset builder: not yet implemented")
        print("See spec section 19-20: Tier A/B/C/D, Parquet shards, train/test split by case/family/time/source to prevent leakage")
    }
}

struct IndexCommand: ParsableCommand {
    func run() throws {
        print("Indexing: not yet implemented")
        print("See spec section 23: BM25, semantic search, hybrid search, citation search, case similarity")
     }
}

// MARK: - Helpers

func print(_ msg: String) {
    // structured log with timestamp
    let ts = ISO8601DateFormatter().string(from: Date())
    fputs("\(ts) \(msg)\n", stderr)
}
