import { useRef, useState, type MouseEvent } from "react";
import { createPortal } from "react-dom";

interface ZoomableImageProps {
  src: string;
  alt: string;
  /** Sizing/shape classes for the base thumbnail (h-*, w-*, rounded-*, etc.). */
  className: string;
  /** How much more detail the zoom pane shows relative to the thumbnail. */
  zoom?: number;
  /** Size (px) of the floating zoom pane. */
  paneSize?: number;
}

interface LensState {
  left: number;
  top: number;
  size: number;
}

interface PaneState {
  left: number;
  top: number;
  backgroundWidth: number;
  backgroundHeight: number;
  backgroundLeft: number;
  backgroundTop: number;
}

/**
 * Hovering shows a lens square that tracks the cursor over the thumbnail, plus a floating pane
 * (rendered via a portal so it always escapes any ancestor's overflow clipping - a scrollable
 * table, a modal, etc.) showing a zoomed-in view of whatever is under the lens. Moving the cursor
 * across the thumbnail pans the zoomed view across the whole image. Touch devices don't get a
 * hover event at all, so this is inert (not broken, just inactive) there - that's an accepted
 * limitation, not a bug to work around.
 */
export function ZoomableImage({ src, alt, className, zoom = 2.5, paneSize = 300 }: ZoomableImageProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [hovering, setHovering] = useState(false);
  const [lens, setLens] = useState<LensState>({ left: 0, top: 0, size: 0 });
  const [pane, setPane] = useState<PaneState | null>(null);

  const updateFromPointer = (clientX: number, clientY: number) => {
    const container = containerRef.current;
    if (!container) {
      return;
    }
    const rect = container.getBoundingClientRect();
    const fractionX = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    const fractionY = Math.min(1, Math.max(0, (clientY - rect.top) / rect.height));

    const lensSize = Math.min(rect.width, rect.height) * 0.5;
    setLens({
      left: Math.min(rect.width - lensSize, Math.max(0, fractionX * rect.width - lensSize / 2)),
      top: Math.min(rect.height - lensSize, Math.max(0, fractionY * rect.height - lensSize / 2)),
      size: lensSize,
    });

    const backgroundWidth = rect.width * zoom;
    const backgroundHeight = rect.height * zoom;
    const rawBackgroundLeft = -(fractionX * backgroundWidth - paneSize / 2);
    const rawBackgroundTop = -(fractionY * backgroundHeight - paneSize / 2);

    const gap = 12;
    const fitsToTheRight = rect.right + gap + paneSize <= window.innerWidth;

    setPane({
      left: fitsToTheRight ? rect.right + gap : Math.max(gap, rect.left - gap - paneSize),
      top: Math.min(window.innerHeight - paneSize - gap, Math.max(gap, rect.top)),
      backgroundWidth,
      backgroundHeight,
      backgroundLeft: Math.min(0, Math.max(paneSize - backgroundWidth, rawBackgroundLeft)),
      backgroundTop: Math.min(0, Math.max(paneSize - backgroundHeight, rawBackgroundTop)),
    });
  };

  const handleMouseEnter = (event: MouseEvent<HTMLDivElement>) => {
    setHovering(true);
    updateFromPointer(event.clientX, event.clientY);
  };

  const handleMouseMove = (event: MouseEvent<HTMLDivElement>) => {
    updateFromPointer(event.clientX, event.clientY);
  };

  return (
    <div
      ref={containerRef}
      className={`relative overflow-hidden ${className}`}
      onMouseEnter={handleMouseEnter}
      onMouseMove={handleMouseMove}
      onMouseLeave={() => setHovering(false)}
    >
      <img src={src} alt={alt} className="h-full w-full object-cover" />
      {hovering && (
        <div
          className="pointer-events-none absolute border-2 border-white bg-white/30 shadow"
          style={{ left: lens.left, top: lens.top, width: lens.size, height: lens.size }}
        />
      )}
      {hovering &&
        pane &&
        createPortal(
          <div
            className="pointer-events-none fixed z-50 overflow-hidden rounded-lg border border-slate-300 bg-white shadow-2xl"
            style={{
              left: pane.left,
              top: pane.top,
              width: paneSize,
              height: paneSize,
              backgroundImage: `url(${src})`,
              backgroundRepeat: "no-repeat",
              backgroundSize: `${pane.backgroundWidth}px ${pane.backgroundHeight}px`,
              backgroundPosition: `${pane.backgroundLeft}px ${pane.backgroundTop}px`,
            }}
          />,
          document.body,
        )}
    </div>
  );
}
