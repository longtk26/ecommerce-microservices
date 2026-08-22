import { apiClient } from "@/lib/api-client";
import type { TShop } from "@/types/shop";

export async function fetchShops(): Promise<TShop[]> {
  return apiClient.get<TShop[]>("/api/inventory/shops");
}
