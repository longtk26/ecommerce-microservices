"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useAuthStore } from "@/store/auth-store";
import { loginSchema, type TLoginFormValues } from "../schemas/auth-schema";

export function useLoginForm(onSuccess?: () => void) {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const loginWithCredentials = useAuthStore((state) => state.loginWithCredentials);

  const form = useForm<TLoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const onSubmit = async (values: TLoginFormValues) => {
    setErrorMessage(null);

    try {
      await loginWithCredentials(values.email, values.password);
      form.reset();
      if (onSuccess) {
        onSuccess();
      }
    } catch (err: unknown) {
      if (err && typeof err === "object" && "message" in err) {
        setErrorMessage(String(err.message));
      } else {
        setErrorMessage("Authentication failed. Please check your credentials.");
      }
    }
  };

  return {
    form,
    onSubmit: form.handleSubmit(onSubmit),
    isSubmitting: form.formState.isSubmitting,
    errors: form.formState.errors,
    errorMessage,
    clearError: () => setErrorMessage(null),
  };
}
