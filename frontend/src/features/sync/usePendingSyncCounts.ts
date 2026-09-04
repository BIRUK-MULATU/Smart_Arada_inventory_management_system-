import { useLiveQuery } from "dexie-react-hooks";
import { db } from "../../db/db";

/** Live counts of this device's queued sales that still need attention. */
export function usePendingSyncCounts() {
  return useLiveQuery(async () => {
    const [pending, failed] = await Promise.all([
      db.syncQueue.where("status").equals("PENDING").count(),
      db.syncQueue.where("status").equals("FAILED").count(),
    ]);
    return { pending, failed };
  }, []);
}
