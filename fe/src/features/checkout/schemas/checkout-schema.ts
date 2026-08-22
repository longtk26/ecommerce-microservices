import { z } from "zod";

export const checkoutSchema = z.object({
  customerName: z
    .string()
    .min(2, "Name must be at least 2 characters")
    .max(50, "Name cannot exceed 50 characters")
    .regex(/^[a-zA-Z0-9\s-]+$/, "Only alphanumeric characters, hyphens, and spaces allowed"),
});

export type TCheckoutFormValues = z.infer<typeof checkoutSchema>;

export function generateUserId(name: string): string {
  const clean = name.trim().toLowerCase().replace(/[^a-z0-9]+/g, "-");
  return `user-${clean || "customer"}`;
}
