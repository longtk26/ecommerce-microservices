import { apiClient } from "@/lib/api-client";

export type TProcessPaymentPayload = {
  orderId: string;
};

export async function processPayment(orderId: string): Promise<void> {
  return apiClient.post<void>("/api/payments", { orderId });
}
