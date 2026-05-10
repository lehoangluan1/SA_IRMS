import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Send } from "lucide-react";

interface NotificationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  recipientName: string;
  availableChannels: Array<{ label: string; value: "sms" | "email" | "in_app" }>;
  onSend: (message: string, channel: "sms" | "email" | "in_app") => void;
}

export function NotificationDialog({
  open,
  onOpenChange,
  recipientName,
  availableChannels,
  onSend,
}: NotificationDialogProps) {
  const [message, setMessage] = useState("");
  const [channel, setChannel] = useState<"sms" | "email" | "in_app">(availableChannels[0]?.value ?? "sms");

  useEffect(() => {
    setChannel(availableChannels[0]?.value ?? "sms");
  }, [availableChannels]);

  useEffect(() => {
    if (!open) {
      setMessage("");
    }
  }, [open]);

  const handleSend = () => {
    onSend(message, channel);
    setMessage("");
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Send Notification</DialogTitle>
          <DialogDescription>
            Send a message to {recipientName}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="notification-channel">Channel</Label>
            <select
              id="notification-channel"
              value={channel}
              onChange={(event) => setChannel(event.target.value as "sms" | "email" | "in_app")}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
            >
              {availableChannels.map((availableChannel) => (
                <option key={availableChannel.value} value={availableChannel.value}>
                  {availableChannel.label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="notification-message">Message</Label>
            <Textarea
              id="notification-message"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Type your message..."
              rows={4}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSend} disabled={message.trim() === ""}>
            <Send className="w-4 h-4 mr-2" />
            Send
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
