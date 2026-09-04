import { useLiveQuery } from "dexie-react-hooks";
import { useEffect } from "react";
import { db } from "../../db/db";
import { useOnlineStatus } from "../../hooks/useOnlineStatus";
import { refreshNotesSnapshot } from "./offlineNotes";

/**
 * Reads notes from the local Dexie cache (always available, works offline) and opportunistically
 * refreshes it from the server on mount and whenever the browser regains connectivity - the same
 * pattern as useOfflineProducts, but merge-based rather than clear-and-replace since notes (unlike
 * the read-only product catalog) can have unsynced local edits that a refresh must not discard.
 */
export function useNotes() {
  const isOnline = useOnlineStatus();

  useEffect(() => {
    if (isOnline) {
      refreshNotesSnapshot();
    }
  }, [isOnline]);

  const notes = useLiveQuery(() => db.notes.orderBy("updatedAtLocal").reverse().toArray(), []);

  return { notes };
}
