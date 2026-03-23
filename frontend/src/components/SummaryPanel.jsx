import { HiSparkles } from 'react-icons/hi';

/**
 * SummaryPanel: Displays the AI-generated executive summary.
 * 
 * This fulfills the "Executive Summary Generation" requirement.
 * The summary comes from Groq's Llama 3.1 model.
 */
export default function SummaryPanel({ summary }) {
  if (!summary) return null;

  return (
    <div className="bg-gradient-to-br from-slate-800 to-slate-800/80 rounded-2xl border border-slate-700 p-6 shadow-xl">
      <h2 className="text-lg font-semibold text-white mb-3 flex items-center gap-2">
        <HiSparkles className="text-yellow-400" />
        Executive Summary
        <span className="text-xs font-normal text-slate-400 bg-slate-700 px-2 py-0.5 rounded-full ml-2">
          AI Generated
        </span>
      </h2>
      <div className="bg-slate-900/60 rounded-xl p-4 border border-slate-700/50">
        <p className="text-slate-300 leading-relaxed text-sm">
          {summary}
        </p>
      </div>
    </div>
  );
}
