import { Check, CheckCircle2, ChevronRight, Globe2, Monitor, SlidersHorizontal, Sun, UserRound } from 'lucide-react';
import { Link } from 'react-router-dom';
import { usePreferences } from '@/app/preferences';
import { Badge, Card } from '@/components/ui';

export function SettingsPage() {
  const { preferences, saved, updatePreferences } = usePreferences();
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Local time';

  return (
    <div className="space-y-7">
      <section className="flex flex-wrap items-start justify-between gap-4 border-b border-ink-200 pb-6">
        <div className="workspace-hero-copy">
          <p className="mb-2 text-xs font-semibold uppercase tracking-[.12em] text-ink-500">Workspace</p>
          <h1 className="font-display text-3xl font-bold tracking-tight text-ink-900">Settings</h1>
          <p className="mt-3 max-w-lg text-sm leading-6 text-ink-600">Manage how your workspace looks and feels on this device.</p>
          <span className="mt-5 inline-flex items-center gap-2 text-xs font-medium text-ink-600" role="status">
            <CheckCircle2 size={15} />{saved ? 'Preferences saved in this browser' : 'Applied for this visit · browser storage is unavailable'}
          </span>
        </div>

      </section>

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <Card className="overflow-hidden">
          <div className="flex items-center gap-3 border-b border-ink-100 px-6 py-5">
            <div className="dimensional-icon bg-ink-50 text-ink-600"><SlidersHorizontal size={20} /></div>
            <div><h2 className="font-display text-lg font-bold text-ink-900">Display preferences</h2><p className="mt-1 text-sm text-ink-500">Appearance, table spacing, and motion.</p></div>
          </div>
          <div className="space-y-7 p-6">
            <div>
              <div className="mb-4 flex items-center justify-between"><h3 className="text-sm font-semibold text-ink-900">Appearance</h3><Badge tone="brand"><Check size={12} className="mr-1" />Light</Badge></div>
              <div className="theme-preview" aria-hidden="true">
                <div className="theme-preview-sidebar"><span /><i /><i /><i /></div>
                <div className="theme-preview-content"><span /><div className="theme-preview-cards"><i /><i /><i /></div><div className="theme-preview-chart"><i /><i /><i /><i /><i /></div></div>
                <div className="theme-preview-sun"><Sun size={24} /></div>
              </div>
              <p className="mt-3 text-xs leading-5 text-ink-500">White surfaces, clear contrast, and a consistent teal accent.</p>
            </div>

            <fieldset className="border-t border-ink-100 pt-6">
              <legend className="sr-only">Table density</legend>
              <h3 className="text-sm font-semibold text-ink-900">Table density</h3>
              <p className="mb-4 mt-1 text-xs leading-5 text-ink-500">Choose the spacing in your data tables.</p>
              <div className="grid grid-cols-1 gap-3 min-[380px]:grid-cols-2">
                {(['comfortable', 'compact'] as const).map((density) => (
                  <label key={density} className="density-option">
                    <input className="peer sr-only" type="radio" name="density" value={density} checked={preferences.density === density} onChange={() => updatePreferences({ density })} />
                    <span className="density-option-body">
                      <span className={`density-lines density-lines-${density}`} aria-hidden="true"><i /><i /><i /></span>
                      <span className="flex items-center justify-between gap-2 text-sm font-semibold"><span>{density === 'comfortable' ? 'Comfortable' : 'Compact'}</span><span className="density-check"><Check size={12} /></span></span>
                    </span>
                  </label>
                ))}
              </div>
            </fieldset>

            <div className="flex items-center justify-between gap-5 border-t border-ink-100 pt-6">
              <div><h3 id="motion-label" className="text-sm font-semibold text-ink-900">Reduce motion</h3><p id="motion-description" className="mt-1 text-xs leading-5 text-ink-500">Keep transitions still. Your device’s reduced-motion setting is always respected.</p></div>
              <button type="button" role="switch" aria-checked={preferences.reduceMotion} aria-labelledby="motion-label" aria-describedby="motion-description" className="preference-switch" onClick={() => updatePreferences({ reduceMotion: !preferences.reduceMotion })}><span /></button>
            </div>
          </div>
        </Card>

        <div className="space-y-5">
          <Card className="p-6">
            <div className="dimensional-icon mb-5 bg-ink-50 text-ink-600"><Globe2 size={20} /></div>
            <h2 className="font-display font-bold text-ink-900">Your local time</h2>
            <p className="mt-2 text-sm leading-6 text-ink-500">Task deadlines follow your device’s time zone.</p>
            <div className="mt-5 rounded-xl border border-ink-200 bg-ink-50 px-4 py-3"><p className="text-[11px] font-semibold uppercase tracking-wider text-ink-500">Time zone</p><p className="mt-1 break-words text-sm font-semibold text-ink-800">{timezone.replace(/_/g, ' ')}</p></div>
          </Card>
          <Link to="/app/profile" className="profile-shortcut group">
            <div className="dimensional-icon bg-ink-50 text-ink-600"><UserRound size={20} /></div>
            <div className="min-w-0 flex-1"><h2 className="text-sm font-bold text-ink-900">Profile & security</h2><p className="mt-1 text-xs leading-5 text-ink-500">Your details and password</p></div>
            <ChevronRight size={18} className="text-ink-400" />
          </Link>
          <p className="flex items-start gap-2 px-2 text-xs leading-5 text-ink-500"><Monitor size={15} className="mt-0.5 shrink-0" />Display preferences apply to this browser. Your teammates keep their own settings.</p>
        </div>
      </div>
    </div>
  );
}
