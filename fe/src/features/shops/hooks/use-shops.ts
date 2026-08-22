"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchShops } from "../api/shops-api";
import { shopKeys } from "../query-keys";
import type { TShop } from "@/types/shop";

export function useShops() {
  return useQuery<TShop[]>({
    queryKey: shopKeys.lists(),
    queryFn: fetchShops,
  });
}
