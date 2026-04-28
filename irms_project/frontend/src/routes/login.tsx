import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { Eye, EyeOff, UtensilsCrossed } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { useAuth } from "@/lib/auth";
import { ApiError } from "@/lib/api/client";

const DEMO_CREDENTIALS: Record<string, { email: string; password: string }> = {
  admin: { email: "admin@irms.io", password: "Password123!" },
  manager: { email: "john.mitchell@irms.io", password: "Password123!" },
  server: { email: "maria.santos@irms.io", password: "Password123!" },
  chef: { email: "carlos.rodriguez@irms.io", password: "Password123!" },
  cashier: { email: "david.ross@irms.io", password: "Password123!" },
  host: { email: "emily.chen@irms.io", password: "Password123!" },
};

export const Route = createFileRoute("/login")({
  component: LoginPage,
  head: () => ({ meta: [{ title: "Sign In - IRMS" }] }),
});

function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [role, setRole] = useState("manager");
  const [email, setEmail] = useState(DEMO_CREDENTIALS.manager.email);
  const [password, setPassword] = useState(DEMO_CREDENTIALS.manager.password);
  const [submitting, setSubmitting] = useState(false);
  const { signIn } = useAuth();

  const applyDemoRole = (nextRole: string) => {
    const demoAccount = DEMO_CREDENTIALS[nextRole] ?? DEMO_CREDENTIALS.manager;
    setRole(nextRole);
    setEmail(demoAccount.email);
    setPassword(demoAccount.password);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 rounded-xl bg-primary flex items-center justify-center mb-4">
            <UtensilsCrossed className="w-6 h-6 text-primary-foreground" />
          </div>
          <h1 className="text-xl font-heading font-bold text-foreground">IRMS</h1>
          <p className="text-sm text-muted-foreground mt-1">Intelligent Restaurant Management System</p>
        </div>

        <div className="bg-card rounded-xl border border-border p-6 shadow-sm">
          <h2 className="text-sm font-heading font-semibold text-foreground mb-4">Sign in to your account</h2>
          <form
            className="space-y-4"
            onSubmit={async (e) => {
              e.preventDefault();
              setSubmitting(true);
              try {
                await signIn({ email, password, role });
                window.location.href = "/";
              } catch (error) {
                const message = error instanceof ApiError ? error.message : "Sign-in failed.";
                toast.error(message);
              } finally {
                setSubmitting(false);
              }
            }}
          >
            <div className="space-y-2">
              <Label>Email</Label>
              <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Password</Label>
              <div className="relative">
                <Input type={showPassword ? "text" : "password"} value={password} onChange={(e) => setPassword(e.target.value)} className="pr-10" />
                <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Role</Label>
              <select value={role} onChange={(e) => applyDemoRole(e.target.value)} className="w-full px-3 py-2 rounded-md border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring">
                <option value="admin">Administrator</option>
                <option value="manager">Manager</option>
                <option value="server">Server</option>
                <option value="chef">Chef</option>
                <option value="cashier">Cashier</option>
                <option value="host">Host</option>
              </select>
            </div>
            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? "Signing In..." : "Sign In"}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
