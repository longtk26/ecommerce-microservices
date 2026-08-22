import { apiClient } from "@/lib/api-client";
import type { TCreateOrderPayload, TCreateOrderResponse } from "@/types/order";

export async function createOrder(payload: TCreateOrderPayload): Promise<TCreateOrderResponse> {
  return apiClient.post<TCreateOrderResponse>("/api/orders", payload);
}
