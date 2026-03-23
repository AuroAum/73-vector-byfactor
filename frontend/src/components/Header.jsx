import { HiDocumentText } from 'react-icons/hi';

/**
 * Header: App title bar with branding.
 * Simple, clean, professional look.
 */
export default function Header() {
  return (
    <header className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 border-b border-slate-700 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-blue-600 p-2 rounded-lg shadow-md">
            <HiDocumentText className="text-white text-2xl" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white tracking-tight">
              DocuScan
            </h1>
            <p className="text-xs text-slate-400">
              OCR-Driven Document Summarization Engine
            </p>
          </div>
        </div>
        <div className="hidden sm:flex items-center gap-2 text-xs text-slate-500">
          <span className="inline-block w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
          Engine Online
        </div>
      </div>
    </header>
  );
}
