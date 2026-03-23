import { useState } from 'react';
import { HiPhotograph, HiDocumentText, HiChevronLeft, HiChevronRight } from 'react-icons/hi';

/**
 * DocumentViewer: Side-by-side split view.
 * NOW with page navigation for multi-page PDFs.
 *
 * LEFT:  Original scanned image (with page navigation)
 * RIGHT: Extracted OCR text (all pages)
 */
export default function DocumentViewer({ result }) {
  const pageUrls = result.pageImageUrls || [result.imageUrl];
  const pageTexts = result.pageTexts || [result.extractedText || ''];
  const totalPages = pageUrls.length;
  const [currentPage, setCurrentPage] = useState(0);
  const currentPageText = pageTexts[currentPage] || '';

  const goToPage = (pageIndex) => {
    if (pageIndex >= 0 && pageIndex < totalPages) {
      setCurrentPage(pageIndex);
    }
  };

  return (
    <div className="bg-slate-800 rounded-2xl border border-slate-700 overflow-hidden shadow-xl">
      {/* Header */}
      <div className="bg-slate-900/80 px-6 py-3 border-b border-slate-700 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-white flex items-center gap-2">
          <HiDocumentText className="text-blue-400" />
          Document Analysis
        </h2>
        <div className="flex items-center gap-3 text-xs text-slate-400">
          <span>{result.fileName}</span>
          {totalPages > 1 && (
            <span className="bg-blue-500/20 text-blue-400 px-2 py-0.5 rounded-full">
              {totalPages} pages
            </span>
          )}
          <span className="bg-green-500/20 text-green-400 px-2 py-0.5 rounded-full">
            {result.processingTimeMs}ms
          </span>
        </div>
      </div>

      {/* Side-by-Side Panels */}
      <div className="grid grid-cols-1 lg:grid-cols-2 divide-y lg:divide-y-0 lg:divide-x divide-slate-700">

        {/* LEFT: Original Image with Page Navigation */}
        <div className="p-4 flex flex-col">
          <div className="flex items-center justify-between min-h-[32px] mb-3">
            <div className="flex items-center gap-2">
              <HiPhotograph className="text-purple-400" />
              <h3 className="text-sm font-medium text-slate-300">Original Document</h3>
            </div>
            
            {/* Page Navigation */}
            {totalPages > 1 && (
              <div className="flex items-center gap-2">
                <button
                  onClick={() => goToPage(currentPage - 1)}
                  disabled={currentPage === 0}
                  className="p-1 rounded bg-slate-700 hover:bg-slate-600 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                >
                  <HiChevronLeft className="text-white text-sm" />
                </button>
                <span className="text-xs text-slate-400 min-w-[60px] text-center">
                  Page {currentPage + 1} / {totalPages}
                </span>
                <button
                  onClick={() => goToPage(currentPage + 1)}
                  disabled={currentPage === totalPages - 1}
                  className="p-1 rounded bg-slate-700 hover:bg-slate-600 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                >
                  <HiChevronRight className="text-white text-sm" />
                </button>
              </div>
            )}
          </div>

          <div className="bg-slate-900 rounded-lg p-2 h-[600px] overflow-auto custom-scrollbar">
            <img
              src={pageUrls[currentPage]}
              alt={`Document page ${currentPage + 1}`}
              className="w-full h-auto rounded"
              loading="lazy"
            />
          </div>

          {/* Page Thumbnails (if multi-page) */}
          {totalPages > 1 && (
            <div className="flex gap-2 mt-3 h-[96px] overflow-x-auto overflow-y-hidden hide-scrollbar items-start">
              {pageUrls.map((url, index) => (
                <button
                  key={index}
                  onClick={() => setCurrentPage(index)}
                  className={`flex-shrink-0 w-16 h-20 rounded-lg border-2 overflow-hidden transition-all
                    ${currentPage === index
                      ? 'border-blue-500 shadow-lg shadow-blue-500/20'
                      : 'border-slate-600 hover:border-slate-400 opacity-60 hover:opacity-100'
                    }`}
                >
                  <img src={url} alt={`Page ${index + 1}`} className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* RIGHT: Extracted Text */}
        <div className="p-4 flex flex-col min-h-0">
          <div className="flex items-center justify-between min-h-[32px] mb-3">
            <div className="flex items-center gap-2">
              <HiDocumentText className="text-cyan-400" />
              <h3 className="text-sm font-medium text-slate-300">Extracted Text (OCR)</h3>
            </div>
            <div className="flex items-center gap-2">
              {totalPages > 1 && (
                <>
                  <button
                    onClick={() => goToPage(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="p-1 rounded bg-slate-700 hover:bg-slate-600 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    <HiChevronLeft className="text-white text-sm" />
                  </button>
                  <span className="text-xs text-slate-400 min-w-[60px] text-center">
                    Page {currentPage + 1} / {totalPages}
                  </span>
                  <button
                    onClick={() => goToPage(currentPage + 1)}
                    disabled={currentPage === totalPages - 1}
                    className="p-1 rounded bg-slate-700 hover:bg-slate-600 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    <HiChevronRight className="text-white text-sm" />
                  </button>
                </>
              )}
              <button
                onClick={() => navigator.clipboard.writeText(currentPageText)}
                className="text-xs text-slate-400 hover:text-white bg-slate-700 hover:bg-slate-600 px-3 py-1 rounded-lg transition-colors"
              >
                Copy Page Text
              </button>
            </div>
          </div>

          <div className="bg-slate-900 rounded-lg p-4 h-[600px] overflow-auto custom-scrollbar">
            <pre className="text-sm text-slate-300 whitespace-pre-wrap font-mono leading-relaxed">
              {(currentPageText && currentPageText.trim())
                ? currentPageText
                : 'No text could be extracted from this page.'}
            </pre>
          </div>

          {/* Text page thumbnails (inter-linked with image thumbnails via shared currentPage state) */}
          {totalPages > 1 && (
            <div className="flex gap-2 mt-3 h-[96px] overflow-x-auto overflow-y-hidden hide-scrollbar max-w-full items-start">
              {pageUrls.map((_, index) => {
                const text = pageTexts[index] || '';
                return (
                <button
                  key={index}
                  onClick={() => setCurrentPage(index)}
                  className={`flex-shrink-0 w-28 h-20 rounded-lg border-2 p-2 text-left transition-all
                    ${currentPage === index
                      ? 'border-cyan-500 shadow-lg shadow-cyan-500/20 bg-cyan-500/10'
                      : 'border-slate-600 hover:border-slate-400 bg-slate-800 opacity-75 hover:opacity-100'
                    }`}
                >
                  <p className="text-[10px] text-slate-400 mb-1">Page {index + 1}</p>
                  <p className="text-[11px] leading-snug text-slate-300 h-[48px] overflow-hidden break-words">
                    {(text && text.trim()) ? text.trim() : 'No OCR text'}
                  </p>
                </button>
              )})}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
