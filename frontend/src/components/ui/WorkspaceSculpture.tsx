import { Check, Layers3 } from 'lucide-react';

/** Decorative CSS sculpture; never overlaps or transforms working controls. */
export function WorkspaceSculpture() {
  return (
    <div className="workspace-sculpture" aria-hidden="true">
      <div className="sculpture-stack">
        <div className="sculpture-layer sculpture-layer-back" />
        <div className="sculpture-layer sculpture-layer-middle" />
        <div className="sculpture-layer sculpture-layer-front">
          <div className="sculpture-mark"><Layers3 size={22} /></div>
          <div className="sculpture-lines"><i /><i /></div>
          <div className="sculpture-bars"><i /><i /><i /><i /></div>
        </div>
      </div>
      <div className="sculpture-check"><Check size={24} strokeWidth={2.5} /></div>
    </div>
  );
}
