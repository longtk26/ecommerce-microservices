"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchOrder } from "../api/orders-api";
import { orderKeys } from "../query-keys";
import type { TGetOrderResponse } from "@/types/order";

export function useOrderStatus(orderId: string) {
  const [pollCount, setPollCount] = React.useState(0);
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
