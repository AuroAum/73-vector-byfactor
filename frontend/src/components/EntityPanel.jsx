import { HiCalendar, HiCurrencyDollar, HiUser, HiMail, HiPhone, HiTag, HiClock, HiHashtag } from 'react-icons/hi';

/**
 * EntityPanel: Displays extracted entities as color-coded cards.
 * 
 * Each entity type gets a unique color and icon for quick visual scanning.
 * This fulfills the "Key Entity Extraction" requirement.
 */

// Map entity types to colors and icons
const ENTITY_CONFIG = {
  DATE:           { color: 'blue',   icon: HiCalendar,       bg: 'bg-blue-500/10',   border: 'border-blue-500/30',   text: 'text-blue-400' },
  AMOUNT:         { color: 'green',  icon: HiCurrencyDollar, bg: 'bg-green-500/10',  border: 'border-green-500/30',  text: 'text-green-400' },
  TOTAL_AMOUNT:   { color: 'emerald',icon: HiCurrencyDollar, bg: 'bg-emerald-500/10',border: 'border-emerald-500/30',text: 'text-emerald-400' },
  DUE_DATE:       { color: 'orange', icon: HiClock,          bg: 'bg-orange-500/10', border: 'border-orange-500/30', text: 'text-orange-400' },
  INVOICE_NUMBER: { color: 'purple', icon: HiHashtag,        bg: 'bg-purple-500/10', border: 'border-purple-500/30', text: 'text-purple-400' },
  SIGNATORY:      { color: 'pink',   icon: HiUser,           bg: 'bg-pink-500/10',   border: 'border-pink-500/30',   text: 'text-pink-400' },
  EMAIL:          { color: 'cyan',   icon: HiMail,           bg: 'bg-cyan-500/10',   border: 'border-cyan-500/30',   text: 'text-cyan-400' },
  PHONE:          { color: 'yellow', icon: HiPhone,          bg: 'bg-yellow-500/10', border: 'border-yellow-500/30', text: 'text-yellow-400' },
};

const DEFAULT_CONFIG = { icon: HiTag, bg: 'bg-slate-500/10', border: 'border-slate-500/30', text: 'text-slate-400' };

export default function EntityPanel({ entities }) {
  if (!entities || entities.length === 0) {
    return (
      <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6 shadow-xl">
        <h2 className="text-lg font-semibold text-white mb-3 flex items-center gap-2">
          <HiTag className="text-amber-400" />
          Key Entities
        </h2>
        <p className="text-sm text-slate-400 italic">
          No specific entities were detected in this document.
        </p>
      </div>
    );
  }

  return (
    <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6 shadow-xl">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-white flex items-center gap-2">
          <HiTag className="text-amber-400" />
          Key Entities
        </h2>
        <span className="text-xs text-slate-400 bg-slate-700 px-2 py-1 rounded-full">
          {entities.length} found
        </span>
      </div>
      
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
        {entities.map((entity, index) => {
          const config = ENTITY_CONFIG[entity.type] || DEFAULT_CONFIG;
          const Icon = config.icon;

          return (
            <div
              key={index}
              className={`${config.bg} ${config.border} border rounded-xl p-3 transition-all hover:scale-[1.02]`}
            >
              <div className="flex items-center gap-2 mb-1">
                <Icon className={`${config.text} text-sm`} />
                <span className={`text-xs font-medium ${config.text} uppercase tracking-wider`}>
                  {entity.label}
                </span>
              </div>
              <p className="text-sm text-white font-medium truncate" title={entity.value}>
                {entity.value}
              </p>
            </div>
          );
        })}
      </div>
    </div>
  );
}
