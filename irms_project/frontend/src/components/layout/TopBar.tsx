import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, LogOut, Menu, User } from "lucide-react";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { useAuth } from "@/lib/auth";
import type { NotificationInboxItem } from "@/lib/api/types";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface TopBarProps {
  onToggleSidebar: () => void;
}

export function TopBar({ onToggleSidebar }: TopBarProps) {
  const queryClient = useQueryClient();
  const { user, signOut } = useAuth();
  const { data: notifications = [] } = useQuery({
    queryKey: ["notifications"],
    queryFn: () => apiFetch<NotificationInboxItem[]>(ENDPOINTS.notifications),
    refetchInterval: 30000,
  });

  const markReadMutation = useMutation({
    mutationFn: (id: string) => apiFetch<NotificationInboxItem>(ENDPOINTS.notificationRead(id), { method: "POST" }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });

  const unreadCount = notifications.filter((item) => !item.readAt).length;

  const openNotification = async (notification: NotificationInboxItem) => {
    if (!notification.readAt) {
      await markReadMutation.mutateAsync(notification.id);
    }
    window.location.assign(resolveNotificationPath(notification));
  };

  const handleSignOut = async () => {
    await signOut();
    queryClient.clear();
    window.location.assign("/login");
  };

  return (
    <header className="flex items-center justify-between h-14 px-6 border-b border-border bg-card shrink-0">
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleSidebar}
          className="p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-secondary transition-colors lg:hidden"
        >
          <Menu className="w-5 h-5" />
        </button>
        <span className="text-xs text-muted-foreground font-mono">
          {new Date().toLocaleDateString("en-US", {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric",
          })}
        </span>
      </div>
      <div className="flex items-center gap-3">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-secondary transition-colors relative">
              <Bell className="w-[18px] h-[18px]" />
              {unreadCount > 0 && <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-destructive rounded-full" />}
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-80">
            <DropdownMenuLabel>Notifications</DropdownMenuLabel>
            <DropdownMenuSeparator />
            {notifications.length === 0 ? (
              <div className="px-3 py-6 text-center text-xs text-muted-foreground">No notifications</div>
            ) : (
              notifications.map((notification) => (
                <DropdownMenuItem
                  key={notification.id}
                  className="flex flex-col items-start gap-1 px-3 py-2 cursor-pointer"
                  onClick={() => void openNotification(notification)}
                >
                  <div className="flex w-full items-center justify-between gap-3">
                    <span className="text-xs font-medium text-foreground">{notification.title}</span>
                    {!notification.readAt && <span className="h-2 w-2 rounded-full bg-primary" />}
                  </div>
                  <span className="text-[11px] leading-4 text-muted-foreground">{notification.body}</span>
                  <span className="text-[10px] text-muted-foreground">{formatRelativeTime(notification.createdAt)}</span>
                </DropdownMenuItem>
              ))
            )}
          </DropdownMenuContent>
        </DropdownMenu>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-secondary transition-colors hover:bg-secondary/80">
              <div className="w-6 h-6 rounded-full bg-primary/20 flex items-center justify-center">
                <User className="w-3.5 h-3.5 text-primary" />
              </div>
              <div className="hidden sm:block text-left">
                <span className="text-xs font-medium text-foreground">
                  {user?.displayName ?? "Team Member"}
                </span>
                <span className="text-[10px] text-muted-foreground ml-1.5">
                  {user?.roles[0] ?? "staff"}
                </span>
              </div>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel>
              <div className="flex flex-col">
                <span className="text-xs font-medium text-foreground">{user?.displayName ?? "Team Member"}</span>
                <span className="text-[10px] text-muted-foreground">{user?.email ?? "Signed in"}</span>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem className="cursor-pointer" onClick={() => void handleSignOut()}>
              <LogOut className="mr-2 h-3.5 w-3.5" />
              Sign Out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}

function resolveNotificationPath(notification: NotificationInboxItem) {
  const reservationId = typeof notification.payload.reservationId === "string" ? notification.payload.reservationId : null;
  const billId = typeof notification.payload.billId === "string" ? notification.payload.billId : null;

  switch (notification.type) {
    case "waitlist_turn_available":
    case "reservation_update":
    case "reservation_check_in_time":
    case "reservation_cancellation":
      return reservationId ? `/reservations#${reservationId}` : "/reservations";
    case "low_stock":
      return "/inventory";
    case "kitchen_status":
    case "food_overdue":
      return "/kitchen";
    case "refund":
    case "receipt_delivery":
      return billId ? `/billing?billId=${billId}` : "/billing";
    case "staff_schedule":
      return "/staff";
    default:
      return "/";
  }
}

function formatRelativeTime(value: string) {
  const minutes = Math.max(0, Math.round((Date.now() - Date.parse(value)) / 60000));
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  return `${hours} hr ago`;
}
