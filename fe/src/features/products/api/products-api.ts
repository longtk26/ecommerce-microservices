import { apiClient } from "@/lib/api-client";
import type { TProduct } from "@/types/product";

export async function fetchProductsByShop(shopId: string): Promise<TProduct[]> {
  return apiClient.get<TProduct[]>(`/api/inventory/shops/${shopId}/products`);
}
