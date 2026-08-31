"use client";

import { useLoginForm } from "../hooks/use-login-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { AlertCircle, Lock, Mail, Sparkles } from "lucide-react";

type LoginFormProps = {
  onSuccess?: () => void;
};

export function LoginForm({ onSuccess }: LoginFormProps) {
  const { form, onSubmit, isSubmitting, errors, errorMessage } = useLoginForm(onSuccess);

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      {errorMessage && (
        <div className="flex items-center gap-2.5 rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive animate-in fade-in duration-200">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Email Input */}
      <div className="space-y-1.5">
        <label
          htmlFor="auth-email"
          className="text-xs font-semibold text-muted-foreground uppercase tracking-wider"
        >
          Email Address
        </label>
        <div className="relative">
          <Mail className="absolute left-3.5 top-3 h-4 w-4 text-muted-foreground" />
          <Input
            id="auth-email"
            type="email"
            placeholder="buyer@example.com"
            className="pl-10"
            {...form.register("email")}
          />
        </div>
        {errors.email && (
          <p className="text-[11px] text-destructive">{errors.email.message}</p>
        )}
      </div>

      {/* Password Input */}
      <div className="space-y-1.5">
        <label
          htmlFor="auth-password"
          className="text-xs font-semibold text-muted-foreground uppercase tracking-wider"
        >
          Password
        </label>
        <div className="relative">
          <Lock className="absolute left-3.5 top-3 h-4 w-4 text-muted-foreground" />
          <Input
            id="auth-password"
            type="password"
            placeholder="••••••••"
            className="pl-10"
            {...form.register("password")}
          />
        </div>
        {errors.password && (
          <p className="text-[11px] text-destructive">{errors.password.message}</p>
        )}
      </div>

      {/* Submit Action */}
      <Button
        type="submit"
        variant="glow"
        size="lg"
        isLoading={isSubmitting}
        className="w-full mt-3 gap-2"
      >
        <Sparkles className="h-4 w-4" />
        <span>Sign In to ShopSaga</span>
      </Button>
    </form>
  );
}
