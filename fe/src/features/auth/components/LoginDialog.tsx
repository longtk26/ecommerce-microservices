"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { LoginForm } from "./LoginForm";
import { LogIn, ShieldCheck } from "lucide-react";

type LoginDialogProps = {
  trigger?: React.ReactNode;
};

export function LoginDialog({ trigger }: LoginDialogProps) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        {trigger ? (
          trigger
        ) : (
          <Button variant="outline" size="sm" className="gap-2 text-xs border-border/80">
            <LogIn className="h-3.5 w-3.5 text-primary" />
            <span>Sign In</span>
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader className="space-y-2">
          <div className="flex items-center gap-2 text-primary">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 border border-primary/20">
              <ShieldCheck className="h-5 w-5 text-primary" />
            </div>
            <div>
              <DialogTitle>Account Sign In</DialogTitle>
              <DialogDescription>
                Sign in to your account to manage orders and checkout.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="mt-2">
          <LoginForm onSuccess={() => setIsOpen(false)} />
        </div>
      </DialogContent>
    </Dialog>
  );
}
