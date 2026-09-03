"use client";

import { useState, useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchOrder } from "../api/orders-api";
import { orderKeys } from "../query-keys";
import { productKeys } from "@/features/products/query-keys";
import type { TGetOrderResponse } from "@/types/order";

export function useOrderStatus(orderId: string) {
  const [pollCount, setPollCount] = useState(0);
  const queryClient = useQueryClient();
  const MAX_POLLS = 30; // 30 * 2s = 60s timeout

  const query = useQuery<TGetOrderResponse>({
    queryKey: orderKeys.detail(orderId),
    queryFn: async () => {
      const data = await fetchOrder(orderId);
      setPollCount((prev) => prev + 1);
      return data;
    },
    enabled: Boolean(orderId),
    refetchInterval: (q) => {
      const order = q.state.data;
      if (!order) return 2000;
      if (order.status === "PENDING" && pollCount < MAX_POLLS) {
        return 2000;
      }
      return false;
    },
  });

  const status = query.data?.status;

  // When order reaches a terminal state, invalidate products to refresh live stock counts
  useEffect(() => {
    if (status === "COMPLETED" || status === "CANCELLED") {
      queryClient.invalidateQueries({ queryKey: productKeys.all });
    }
  }, [status, queryClient]);

  const isTimeout =
    query.data?.status === "PENDING" && pollCount >= MAX_POLLS;

  return {
    order: query.data,
    status: query.data?.status,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
    isPolling: query.data?.status === "PENDING" && !isTimeout,
    isTimeout,
    pollCount,
  };
}

