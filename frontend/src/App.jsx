import { useState, useEffect } from 'react';
import Header from './components/Header';
import UploadZone from './components/UploadZone';
import DocumentViewer from './components/DocumentViewer';
import EntityPanel from './components/EntityPanel';
import SummaryPanel from './components/SummaryPanel';
import { uploadDocuments, checkHealth } from './services/api';
import { HiRefresh, HiExclamationCircle } from 'react-icons/hi';

/**
 * App: Main orchestrator component.
 * 
 * Manages the application state machine:
 * 1. IDLE → waiting for file upload
 * 2. UPLOADING → file being sent to server (progress bar)
 * 3. PROCESSING → server doing OCR + analysis
 * 4. DONE → showing results
 * 5. ERROR → something went wrong
 */
export default function App() {
  const [status, setStatus] = useState('idle'); // idle | uploading | processing | done | error
  const [progress, setProgress] = useState(0);
  const [results, setResults] = useState([]);
  const [errors, setErrors] = useState([]);
  const [activeResultIndex, setActiveResultIndex] = useState(0);
  const [error, setError] = useState('');
  const [backendOnline, setBackendOnline] = useState(false);

  // Check if backend is online on load
  useEffect(() => {
    checkHealth().then((data) => {
      setBackendOnline(data !== null);
    });
  }, []);

  const handleFilesSelected = async (files) => {
    setStatus('uploading');
    setProgress(0);
    setError('');
    setResults([]);
    setErrors([]);
    setActiveResultIndex(0);

    try {
      // Upload phase
      setProgress(10);
      
      const onProgress = (percent) => {
        // Upload progress takes 0-50% of the bar
        setProgress(Math.min(percent / 2, 50));
      };

      // When upload completes, switch to processing phase
      setStatus('processing');
      setProgress(55);

      const data = await uploadDocuments(files, onProgress);

      if (!data.results || data.results.length === 0) {
        throw new Error('No documents were processed successfully');
      }

      setProgress(100);
      setResults(data.results);
      setErrors(data.errors || []);
      setStatus('done');
    } catch (err) {
      setError(err.message || 'An unexpected error occurred');
      setStatus('error');
    }
  };

  const handleReset = () => {
    setStatus('idle');
    setProgress(0);
    setResults([]);
    setErrors([]);
    setActiveResultIndex(0);
    setError('');
  };

  const activeResult = results[activeResultIndex] || null;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950">
      <Header />

      <main className="max-w-7xl mx-auto px-4 py-8">
        
        {/* Backend offline warning */}
        {!backendOnline && (
          <div className="mb-6 bg-red-500/10 border border-red-500/30 rounded-xl p-4 flex items-center gap-3">
            <HiExclamationCircle className="text-red-400 text-xl flex-shrink-0" />
            <div>
              <p className="text-red-400 font-medium text-sm">Backend server is not responding</p>
              <p className="text-red-400/70 text-xs">Make sure Spring Boot is running on port 8080</p>
            </div>
          </div>
        )}

        {/* Upload Zone — shown when idle or error */}
        {(status === 'idle' || status === 'error') && (
          <div className="max-w-3xl mx-auto">
            <div className="text-center mb-8">
              <h2 className="text-3xl font-bold text-white mb-2">
                Digitize Your Documents
              </h2>
              <p className="text-slate-400">
                Upload one or more scanned documents and get instant OCR text extraction, 
                key entity detection, and an AI-powered summary.
              </p>
            </div>
            
            <UploadZone onFilesSelected={handleFilesSelected} isProcessing={false} />

            {/* Error message */}
            {status === 'error' && (
              <div className="mt-4 bg-red-500/10 border border-red-500/30 rounded-xl p-4">
                <p className="text-red-400 text-sm flex items-center gap-2">
                  <HiExclamationCircle />
                  {error}
                </p>
              </div>
            )}
          </div>
        )}

        {/* Processing State */}
        {(status === 'uploading' || status === 'processing') && (
          <div className="max-w-md mx-auto text-center">
            <div className="bg-slate-800 rounded-2xl border border-slate-700 p-8 shadow-xl animate-pulse-glow">
              {/* Spinner */}
              <div className="flex justify-center mb-6">
                <div className="w-16 h-16 border-4 border-slate-600 border-t-blue-500 rounded-full animate-spin"></div>
              </div>
              
              <h3 className="text-lg font-semibold text-white mb-2">
                {status === 'uploading' ? 'Uploading Document...' : 'Processing Document...'}
              </h3>
              <p className="text-sm text-slate-400 mb-4">
                {status === 'uploading'
                  ? 'Sending file to server'
                  : 'Running OCR, extracting entities, generating summary'
                }
              </p>

              {/* Progress bar */}
              <div className="w-full bg-slate-700 rounded-full h-2">
                <div
                  className="bg-blue-500 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${progress}%` }}
                ></div>
              </div>
              <p className="text-xs text-slate-500 mt-2">{Math.round(progress)}%</p>
            </div>
          </div>
        )}

        {/* Results */}
        {status === 'done' && activeResult && (
          <div className="space-y-6">
            {/* New Document Button */}
            <div className="flex justify-between items-center">
              <h2 className="text-xl font-bold text-white">Results</h2>
              <button
                onClick={handleReset}
                className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors text-sm font-medium"
              >
                <HiRefresh />
                Process New Document
              </button>
            </div>

            {/* Batch Result Picker */}
            {results.length > 1 && (
              <div className="bg-slate-800 rounded-2xl border border-slate-700 p-4 shadow-xl">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="text-sm font-semibold text-white">Processed Documents</h3>
                  <span className="text-xs text-slate-400 bg-slate-700 px-2 py-0.5 rounded-full">
                    {results.length} successful
                  </span>
                </div>
                <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-1">
                  {results.map((item, index) => (
                    <button
                      key={`${item.fileName}-${index}`}
                      onClick={() => setActiveResultIndex(index)}
                      className={`flex-shrink-0 px-3 py-2 rounded-lg border text-sm transition-colors ${
                        activeResultIndex === index
                          ? 'bg-blue-600/20 border-blue-500 text-blue-300'
                          : 'bg-slate-900 border-slate-700 text-slate-300 hover:border-slate-500'
                      }`}
                    >
                      {item.fileName}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Partial Failures */}
            {errors.length > 0 && (
              <div className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-4">
                <p className="text-amber-300 text-sm font-medium mb-2">
                  Some files could not be processed:
                </p>
                <ul className="text-xs text-amber-200/90 space-y-1">
                  {errors.map((item, index) => (
                    <li key={`${item.fileName}-${index}`}>{item.fileName}: {item.message}</li>
                  ))}
                </ul>
              </div>
            )}

            {/* Executive Summary */}
            <SummaryPanel summary={activeResult.summary} />

            {/* Key Entities */}
            <EntityPanel entities={activeResult.entities} />

            {/* Side-by-Side Viewer */}
            <DocumentViewer result={activeResult} />
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="text-center py-6 text-xs text-slate-600">
        DocuScan — National Hackathon 2026 | OCR-Driven Enterprise Document Summarization Engine
      </footer>
    </div>
  );
}
