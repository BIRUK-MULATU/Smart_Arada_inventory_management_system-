import { useEffect, useState } from "react";

/**
 * navigator.onLine only reflects the network adapter's state, not whether the backend is
 * actually reachable - a device can report "online" while the API server is unreachable. That's
 * a known limitation, acceptable as the starting signal for this phase; a more robust
 * connectivity probe (actively pinging the backend) can be layered on later if needed.
 */
export function useOnlineStatus(): boolean {
  const [isOnline, setIsOnline] = useState(() => navigator.onLine);

  useEffect(() => {
    const goOnline = () => setIsOnline(true);
    const goOffline = () => setIsOnline(false);
    window.addEventListener("online", goOnline);
    window.addEventListener("offline", goOffline);
    return () => {
      window.removeEventListener("online", goOnline);
      window.removeEventListener("offline", goOffline);
    };
  }, []);

  return isOnline;
}
