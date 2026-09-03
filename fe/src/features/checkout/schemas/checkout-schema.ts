import { z } from "zod";

export const checkoutSchema = z.object({
  customerEmail: z
    .string()
    .min(1, "Email address is required")
    .email("Please enter a valid email address"),
});

export type TCheckoutFormValues = z.infer<typeof checkoutSchema>;

export function generateUserId(email: string): string {
  const clean = email.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-");
  return `user-${clean || "customer"}`;
}

