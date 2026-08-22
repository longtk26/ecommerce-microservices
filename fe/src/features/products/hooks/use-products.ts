"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchProductsByShop } from "../api/products-api";
import { productKeys } from "../query-keys";
import type { TProduct } from "@/types/product";

export function useShopProducts(shopId: string) {
  return useQuery<TProduct[]>({
    queryKey: productKeys.byShop(shopId),
    queryFn: () => fetchProductsByShop(shopId),
    enabled: Boolean(shopId),
  });
}
