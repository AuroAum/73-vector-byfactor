import { useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { HiCloudUpload, HiPhotograph, HiDocumentText } from 'react-icons/hi';

/**
 * UploadZone: Drag-and-drop file upload area.
 * 
 * Uses react-dropzone library which handles:
 * - Drag over/leave events
 * - File type validation
 * - Click to browse
 * - Multiple browser compatibility
 */
export default function UploadZone({ onFilesSelected, isProcessing }) {
  
  const onDrop = useCallback((acceptedFiles) => {
    if (acceptedFiles.length > 0) {
      onFilesSelected(acceptedFiles);
    }
  }, [onFilesSelected]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'image/png': ['.png'],
      'image/jpeg': ['.jpg', '.jpeg'],
    },
    maxFiles: 10,
    maxSize: 50 * 1024 * 1024, // 50MB
    disabled: isProcessing,
  });

  return (
    <div
      {...getRootProps()}
      className={`
        relative border-2 border-dashed rounded-2xl p-12 text-center cursor-pointer
        transition-all duration-300 ease-out
        ${isDragActive
          ? 'border-blue-400 bg-blue-500/10 scale-[1.02] shadow-xl shadow-blue-500/20'
          : 'border-slate-600 bg-slate-800/50 hover:border-blue-500 hover:bg-slate-800'
        }
        ${isProcessing ? 'opacity-50 cursor-not-allowed' : ''}
      `}
    >
      <input {...getInputProps()} />
      
      <div className="flex flex-col items-center gap-4">
        {isDragActive ? (
          <>
            <HiCloudUpload className="text-6xl text-blue-400 animate-bounce" />
            <p className="text-xl font-semibold text-blue-400">
              Drop your document here!
            </p>
          </>
        ) : (
          <>
            <div className="flex items-center gap-3">
              <HiCloudUpload className="text-5xl text-slate-400" />
            </div>
            <div>
              <p className="text-lg font-semibold text-slate-300">
                Drag & drop your scanned document here
              </p>
              <p className="text-sm text-slate-500 mt-1">
                or click to browse files
              </p>
            </div>
            <div className="flex items-center gap-4 mt-2">
              <div className="flex items-center gap-1 text-xs text-slate-500 bg-slate-700/50 px-3 py-1.5 rounded-full">
                <HiDocumentText className="text-red-400" />
                PDF
              </div>
              <div className="flex items-center gap-1 text-xs text-slate-500 bg-slate-700/50 px-3 py-1.5 rounded-full">
                <HiPhotograph className="text-green-400" />
                PNG
              </div>
              <div className="flex items-center gap-1 text-xs text-slate-500 bg-slate-700/50 px-3 py-1.5 rounded-full">
                <HiPhotograph className="text-yellow-400" />
                JPG
              </div>
            </div>
            <p className="text-xs text-slate-600 mt-1">Up to 10 files, 50MB each</p>
          </>
        )}
      </div>
    </div>
  );
}
