import { useCallback, useEffect, useRef, useState, type MouseEvent } from "react";
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

const TOUCH_HOLD_DELAY_MS = 400;
const TOUCH_MOVE_CANCEL_THRESHOLD_PX = 10;

/**
 * Hovering (mouse) or tapping and holding (touch) shows a lens square that tracks the pointer
 * over the thumbnail, plus a floating pane (rendered via a portal so it always escapes any
 * ancestor's overflow clipping - a scrollable table, a modal, etc.) showing a zoomed-in view of
 * whatever is under the lens. Moving the pointer across the thumbnail pans the zoomed view across
 * the whole image.
 *
 * Touch uses a real long-press: a quick tap or a swipe (scrolling the page/table) never triggers
 * it, only holding still past TOUCH_HOLD_DELAY_MS does. Once held, panning the finger is
 * intercepted (preventDefault on that one touchmove) so the page doesn't scroll out from under the
 * zoom pane while the user is dragging it around - before the hold fires, touches are left alone
 * so normal scrolling/tapping is completely unaffected.
 */
export function ZoomableImage({ src, alt, className, zoom = 2.5, paneSize = 300 }: ZoomableImageProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [hovering, setHovering] = useState(false);
  const [lens, setLens] = useState<LensState>({ left: 0, top: 0, size: 0 });
  const [pane, setPane] = useState<PaneState | null>(null);

  const updateFromPointer = useCallback(
    (clientX: number, clientY: number) => {
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
    },
    [zoom, paneSize],
  );

  const handleMouseEnter = (event: MouseEvent<HTMLDivElement>) => {
    setHovering(true);
    updateFromPointer(event.clientX, event.clientY);
  };

  const handleMouseMove = (event: MouseEvent<HTMLDivElement>) => {
    updateFromPointer(event.clientX, event.clientY);
  };

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }

    let holdTimeout: number | null = null;
    let holdActive = false;
    let touchStart: { x: number; y: number } | null = null;

    const clearHoldTimeout = () => {
      if (holdTimeout !== null) {
        window.clearTimeout(holdTimeout);
        holdTimeout = null;
      }
    };

    const endHold = () => {
      clearHoldTimeout();
      holdActive = false;
      touchStart = null;
      setHovering(false);
    };

    const onTouchStart = (event: TouchEvent) => {
      if (event.touches.length !== 1) {
        return;
      }
      const touch = event.touches[0];
      touchStart = { x: touch.clientX, y: touch.clientY };
      clearHoldTimeout();
      holdTimeout = window.setTimeout(() => {
        holdActive = true;
        setHovering(true);
        updateFromPointer(touch.clientX, touch.clientY);
      }, TOUCH_HOLD_DELAY_MS);
    };

    const onTouchMove = (event: TouchEvent) => {
      const touch = event.touches[0];
      if (!touch) {
        return;
      }
      if (holdActive) {
        // The magnifier is engaged - pan it, and stop the page from scrolling underneath it.
        event.preventDefault();
        updateFromPointer(touch.clientX, touch.clientY);
        return;
      }
      // Not holding yet: if the finger has already moved enough, this is a scroll/swipe, not a
      // long press, so cancel the pending hold and leave the touch alone.
      if (touchStart) {
        const dx = touch.clientX - touchStart.x;
        const dy = touch.clientY - touchStart.y;
        if (Math.hypot(dx, dy) > TOUCH_MOVE_CANCEL_THRESHOLD_PX) {
          clearHoldTimeout();
        }
      }
    };

    container.addEventListener("touchstart", onTouchStart, { passive: true });
    container.addEventListener("touchmove", onTouchMove, { passive: false });
    container.addEventListener("touchend", endHold);
    container.addEventListener("touchcancel", endHold);

    return () => {
      clearHoldTimeout();
      container.removeEventListener("touchstart", onTouchStart);
      container.removeEventListener("touchmove", onTouchMove);
      container.removeEventListener("touchend", endHold);
      container.removeEventListener("touchcancel", endHold);
    };
  }, [updateFromPointer]);

  return (
    <div
      ref={containerRef}
      className={`relative touch-pan-y overflow-hidden ${className}`}
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
