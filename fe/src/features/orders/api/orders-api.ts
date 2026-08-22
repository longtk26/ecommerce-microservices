import { apiClient } from "@/lib/api-client";
import type { TGetOrderResponse } from "@/types/order";

export async function fetchOrder(orderId: string): Promise<TGetOrderResponse> {
  return apiClient.get<TGetOrderResponse>(`/api/orders/${orderId}`);
}
