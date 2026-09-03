import { Badge } from "./Badge";
import type { SyncStatus } from "../db/types";

const toneByStatus: Record<SyncStatus, "neutral" | "success" | "warning" | "danger"> = {
  PENDING: "neutral",
  SYNCING: "warning",
  SYNCED: "success",
  FAILED: "danger",
  CONFLICT: "danger",
};

const labelByStatus: Record<SyncStatus, string> = {
  PENDING: "Saved on this device",
  SYNCING: "Syncing…",
  SYNCED: "Recorded",
  FAILED: "Sync failed",
  CONFLICT: "Needs review",
};

export function SyncStatusBadge({ status }: { status: SyncStatus }) {
  return <Badge tone={toneByStatus[status]}>{labelByStatus[status]}</Badge>;
}
