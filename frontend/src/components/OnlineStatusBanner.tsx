import { useOnlineStatus } from "../hooks/useOnlineStatus";

export function OnlineStatusBanner() {
  const isOnline = useOnlineStatus();

  if (isOnline) {
    return null;
  }

  return (
    <div role="status" className="bg-amber-100 px-4 py-2 text-center text-sm font-medium text-amber-800">
      You're offline. Sales will be saved on this device and synced when you're back online.
    </div>
  );
}
