"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { processPayment } from "../api/payment-api";
import { orderKeys } from "../query-keys";

export function useProcessPayment(orderId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => processPayment(orderId),
    onSuccess: () => {
      // Invalidate and trigger immediate refetch of order status
      queryClient.invalidateQueries({ queryKey: orderKeys.detail(orderId) });
    },
  });
}
